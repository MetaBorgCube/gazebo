package nl.jochembroekhoff.gazebo.standalone.cli

import nl.jochembroekhoff.gazebo.standalone.lib.*
import nl.jochembroekhoff.gazebo.standalone.lib.tasks.EmitStxLib
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.resource.IResourceService
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

    @Option(
        names = ["--internal-action"],
        arity = "0..1",
        hidden = true,
    )
    var internalAction: InternalAction = InternalAction.DEFAULT

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

        when (internalAction) {
            InternalAction.DEFAULT -> {
                GazeboSpoofaxFactory.createGazeboSpoofax(
                    GazeboProjectConfigServiceConfig(
                        libs = setOf("std.mcje.1.17.1+0")
                    )
                ).use { spoofax ->
                    GazeboRunner(runnerConfig).run(spoofax)
                    // TODO: get error messages and print them here, instead of letting GazeboRunner do it
                }
            }
            InternalAction.STDLIB -> {
                GazeboSpoofaxFactory.createGazeboSpoofax(
                    GazeboProjectConfigServiceConfig(
                        languages = setOf(GazeboLang.GZB, GazeboLang.GZBC),
                        extensions = setOf(GazeboExt.GZB2GZBC)
                    )
                ).use { spoofax ->
                    GazeboRunner(runnerConfig)
                        .withAdditionalTask(EmitStxLib(GazeboLang.GZB))
                        .withAdditionalTask(EmitStxLib(GazeboLang.GZBC))
                        .run(spoofax)
                    // TODO: get error messages and print them here, instead of letting GazeboRunner do it
                }
            }
        }

        val sec = timer.stop() / 1e9
        logger.info("Finished after {} seconds", sec)

        return 0
    }
}
