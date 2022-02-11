package nl.jochembroekhoff.gazebo.standalone.lib

import nl.jochembroekhoff.gazebo.standalone.lib.tasks.AdditionalTask
import org.metaborg.core.action.CompileGoal
import org.metaborg.core.action.NamedGoal
import org.metaborg.core.build.BuildInputBuilder
import org.metaborg.core.messages.WithLocationStreamMessagePrinter
import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput
import org.metaborg.spoofax.core.shell.CLIUtils
import org.metaborg.util.log.LoggerUtils

class GazeboRunner(private val configuration: GazeboRunnerConfiguration) {

    private val logger = LoggerUtils.logger(javaClass)

    private var additionalTasks = mutableListOf<AdditionalTask<*>>()

    private fun loadLanguageImpl(spoofax: Spoofax) {
        configuration.languageArchiveProvider(spoofax.resourceService).forEach { languageArchive ->
            if (languageArchive.isFolder) {
                spoofax.languageDiscoveryService.languageFromDirectory(languageArchive)
            } else {
                spoofax.languageDiscoveryService.languageFromArchive(languageArchive)
            }
        }
    }

    private fun buildProject(spoofax: Spoofax, project: IProject): ISpoofaxBuildOutput? {
        val buildInput = BuildInputBuilder(project)
            .withTransformGoals(listOf(
                CompileGoal(),
                // FIXME: adding this explicit NamedGoal is necessary because the LLMC language doesn't have analysis
                //  and it is currently impossible to disable the analysis requirement for "on save" handlers,
                //  i.e. compile goals
                NamedGoal(listOf("Minecraft: Java Edition", "Generate function (shared)"))
            ))
            .withSourcesFromDefaultSourceLocations(true)
            .withMessagePrinter(HtmlUnescapeMessagePrinter(WithLocationStreamMessagePrinter(spoofax.sourceTextService, spoofax.projectService, System.out)))
            .build(spoofax.dependencyService, spoofax.languagePathService)

        val buildTask = spoofax.processorRunner.build(buildInput, null, null)
        return buildTask.schedule().block().result()
    }

    fun withAdditionalTask(additionalTask: AdditionalTask<*>): GazeboRunner {
        additionalTasks.add(additionalTask)
        return this
    }

    fun run(spoofax: Spoofax) {
        val cli = CLIUtils(spoofax)

        loadLanguageImpl(spoofax)
        val project = cli.getOrCreateProject(spoofax.resolve(configuration.root))

        if (project == null) {
            logger.error("Project null")
            return
        }

        val output = buildProject(spoofax, project)

        if (output == null) {
            logger.error("Output null")
            return
        }

        if (!output.success()) {
            logger.error("Build did not succeed")
        }

        additionalTasks.forEach {
            it.run(spoofax, cli, project, output)
        }
    }
}
