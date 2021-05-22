package nl.jochembroekhoff.gazebo.standalone.lib

import org.metaborg.core.build.BuildInputBuilder
import org.metaborg.core.project.IProject
import org.metaborg.core.project.ISimpleProjectService
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.util.log.LoggerUtils

class GazeboRunner(private val configuration: GazeboRunnerConfiguration) {

    private val logger = LoggerUtils.logger(javaClass)

    private fun loadLanguageImpl(spoofax: Spoofax) {
        configuration.languageArchiveProvider(spoofax.resourceService).forEach { languageArchive ->
            spoofax.languageDiscoveryService.languageFromArchive(languageArchive)
        }
    }

    private fun loadProject(spoofax: Spoofax): IProject? {
        val resolved = spoofax.resourceService.resolve(configuration.root)
        return spoofax.injector.getInstance(ISimpleProjectService::class.java).create(resolved)
    }

    fun run(spoofax: Spoofax) {
        loadLanguageImpl(spoofax)
        val project = loadProject(spoofax)

        if (project == null) {
            logger.error("Project null")
            return
        }

        val buildInput = BuildInputBuilder(project)
            .withSourcesFromDefaultSourceLocations(true)
            .build(spoofax.dependencyService, spoofax.languagePathService)

        val buildTask = spoofax.processorRunner.build(buildInput, null, null)
        val output = buildTask.schedule().block().result()
        if (output == null) {
            logger.error("Output null")
            return
        }

        if (!output.success()) {
            logger.error("Failed running the build")
            output.allMessages().forEach { message ->
                println(message.message())
                message.source()?.let(::println)
                message.region()?.let(::println)
                println()
            }
            return
        }
    }
}
