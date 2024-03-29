package nl.jochembroekhoff.gazebo.standalone.lib.runner

import nl.jochembroekhoff.gazebo.standalone.lib.messages.HtmlUnescapeMessagePrinter
import nl.jochembroekhoff.gazebo.standalone.lib.messages.AggregateMessagePrinter
import nl.jochembroekhoff.gazebo.standalone.lib.tasks.OverlayTask
import org.metaborg.core.action.CompileGoal
import org.metaborg.core.action.NamedGoal
import org.metaborg.core.build.BuildInputBuilder
import org.metaborg.core.build.CommonPaths
import org.metaborg.core.messages.IMessagePrinter
import org.metaborg.core.messages.WithLocationStreamMessagePrinter
import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput
import org.metaborg.spoofax.core.shell.CLIUtils
import org.metaborg.util.log.LoggerUtils
import org.metaborg.util.task.IProgress

class GazeboRunner(private val configuration: GazeboRunnerConfiguration) {

    private val logger = LoggerUtils.logger(javaClass)

    private var overlayTasks = mutableListOf<OverlayTask<*>>()

    private fun buildProject(spoofax: Spoofax, project: IProject, progress: IProgress?): ISpoofaxBuildOutput? {
        val messagePrinters = buildList<IMessagePrinter> {
            if (configuration.logToStdout) {
                add(HtmlUnescapeMessagePrinter(WithLocationStreamMessagePrinter(spoofax.sourceTextService, spoofax.projectService, System.out)))
            }
            if (configuration.logToFile) {
                val msgFile = CommonPaths(project.location()).targetDir().resolveFile("gzbs-messages.txt")
                val os = msgFile.content.outputStream
                add(HtmlUnescapeMessagePrinter(WithLocationStreamMessagePrinter(spoofax.sourceTextService, spoofax.projectService, os)))
            }
            if (configuration.logAux != null) {
                add(HtmlUnescapeMessagePrinter(configuration.logAux))
            }
        }

        val buildInput = BuildInputBuilder(project)
            .withTransformGoals(listOf(
                CompileGoal(),
                // FIXME: adding this explicit NamedGoal is necessary because the LLMC language doesn't have analysis
                //  and it is currently impossible to disable the analysis requirement for "on save" handlers,
                //  i.e. compile goals
                NamedGoal(listOf("Minecraft: Java Edition", "Generate function (shared)"))
            ))
            .withSourcesFromDefaultSourceLocations(true)
            .withDefaultIncludePaths(false)
            .withMessagePrinter(AggregateMessagePrinter(messagePrinters))
            .build(spoofax.dependencyService, spoofax.languagePathService)

        val buildTask = spoofax.processorRunner.build(buildInput, progress, null)
        return buildTask.schedule().block().result()
    }

    fun withOverlayTask(overlayTask: OverlayTask<*>): GazeboRunner {
        overlayTasks.add(overlayTask)
        return this
    }

    fun run(spoofax: Spoofax, progress: IProgress? = null): Boolean {
        val cli = CLIUtils(spoofax)

        val project = cli.getOrCreateProject(spoofax.resolve(configuration.root))

        if (project == null) {
            logger.error("Project null")
            return false
        }

        val output = buildProject(spoofax, project, progress)

        if (output == null) {
            logger.error("Output null")
            return false
        }

        if (!output.success()) {
            logger.error("Build did not succeed")
            return false
        }

        overlayTasks.forEach {
            it.run(spoofax, cli, project, output)
        }

        return true
    }
}
