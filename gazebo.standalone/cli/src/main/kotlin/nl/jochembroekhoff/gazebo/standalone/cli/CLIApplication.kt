package nl.jochembroekhoff.gazebo.standalone.cli

import nl.jochembroekhoff.gazebo.standalone.lib.GazeboModule
import nl.jochembroekhoff.gazebo.standalone.lib.GazeboRunner
import nl.jochembroekhoff.gazebo.standalone.lib.GazeboRunnerConfiguration
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.resource.IResourceService
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.util.log.LoggerUtils
import org.metaborg.util.time.Timer
import picocli.CommandLine.*
import java.io.File
import java.util.concurrent.Callable

@Command(
    mixinStandardHelpOptions = true,
    versionProvider = ImplementationVersionProvider::class,
)
class CLIApplication : Callable<Int> {

    private val logger = LoggerUtils.logger(javaClass)

    @Parameters(
        description = ["Project root directory, containing data/ and pack.mcmeta"],
        arity = "0..1",
    )
    var root: File = File(".")

    @Option(
        names = ["--language-archive"],
        arity = "*",
    )
    var languageArchives: List<File> = listOf()

    private fun languageArchiveProvider(resourceService: IResourceService): Collection<FileObject> {
        return if (languageArchives.isNotEmpty()) {
            languageArchives.map(resourceService::resolve)
        } else {
            TODO("Load language archive from jar resources")
        }
    }

    private fun createRunnerConfiguration(): GazeboRunnerConfiguration {
        return GazeboRunnerConfiguration(
            root,
            ::languageArchiveProvider,
        )
    }

    override fun call(): Int {
        val timer = Timer(true)

        val runnerConfig = createRunnerConfiguration()

        Spoofax(GazeboModule()).use { spoofax ->
            GazeboRunner(runnerConfig).run(spoofax)
            // TODO: get error messages and print them here, instead of letting GazeboRunner do it
        }

        val sec = timer.stop() / 1e9
        logger.info("Finished after {} seconds", sec)

        return 0
    }
}
