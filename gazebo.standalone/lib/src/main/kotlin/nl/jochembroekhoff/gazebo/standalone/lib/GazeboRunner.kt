package nl.jochembroekhoff.gazebo.standalone.lib

import org.apache.commons.io.IOUtils
import org.metaborg.core.action.CompileGoal
import org.metaborg.core.build.BuildInputBuilder
import org.metaborg.core.messages.WithLocationStreamMessagePrinter
import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput
import org.metaborg.spoofax.core.shell.CLIUtils
import org.metaborg.util.log.LoggerUtils
import org.metaborg.util.time.Timer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GazeboRunner(private val configuration: GazeboRunnerConfiguration) {

    private val logger = LoggerUtils.logger(javaClass)

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
            .withTransformGoals(listOf(CompileGoal()))
            .withSourcesFromDefaultSourceLocations(true)
            .withMessagePrinter(HtmlUnescapeMessagePrinter(WithLocationStreamMessagePrinter(spoofax.sourceTextService, spoofax.projectService, System.out)))
            .build(spoofax.dependencyService, spoofax.languagePathService)

        val buildTask = spoofax.processorRunner.build(buildInput, null, null)
        return buildTask.schedule().block().result()
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
    }

    fun tempStxLib(spoofax: Spoofax) {
        val cli = CLIUtils(spoofax)

        loadLanguageImpl(spoofax)
        val project = cli.getOrCreateProject(spoofax.resolve(configuration.root))

        val gazebo = cli.getLanguage("gazebo")
        val stxlib = cli.getNamedTransformAction("Make project library", gazebo)

        if (project == null) {
            logger.error("Project null")
            return
        }

        val output = buildProject(spoofax, project)
        if (output == null) {
            logger.error("Output null")
            return
        }

        val stxlibTimer = Timer(true)
        val firstAnalysis = output.analysisResults().first()
        val res = cli.transform(firstAnalysis, stxlib, firstAnalysis.context())
        logger.info("created stxlib in {} seconds", stxlibTimer.stop() / 1e9)
        val exec = Executors.newFixedThreadPool(2)
        exec.submit {
            project.location().resolveFile("target/out.pretty.stxlib").content.outputStream.use {
                IOUtils.write(spoofax.strategoCommon.toString(res), it, Charsets.UTF_8)
            }
        }
        exec.submit {
            project.location().resolveFile("target/out.stxlib").content.outputStream.writer().use {
                res.writeAsString(it)
            }
        }
        exec.shutdown()
        exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)

        if (!output.success()) {
            logger.error("Build did not succeed")
        }
    }
}
