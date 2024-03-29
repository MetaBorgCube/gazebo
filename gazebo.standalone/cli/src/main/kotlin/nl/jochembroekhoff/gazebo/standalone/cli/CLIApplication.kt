package nl.jochembroekhoff.gazebo.standalone.cli

import nl.jochembroekhoff.gazebo.standalone.lib.*
import nl.jochembroekhoff.gazebo.standalone.lib.constants.GazeboExt
import nl.jochembroekhoff.gazebo.standalone.lib.constants.GazeboLang
import nl.jochembroekhoff.gazebo.standalone.lib.project.GazeboProjectConfigServiceConfig
import nl.jochembroekhoff.gazebo.standalone.lib.runner.GazeboRunner
import nl.jochembroekhoff.gazebo.standalone.lib.runner.GazeboRunnerConfiguration
import nl.jochembroekhoff.gazebo.standalone.lib.tasks.EmitStxLib
import nl.jochembroekhoff.gazebo.standalone.lib.tasks.TaskUtil.chain
import nl.jochembroekhoff.gazebo.standalone.lib.tasks.datapack.CompressDataPackTask
import nl.jochembroekhoff.gazebo.standalone.lib.tasks.datapack.EmitDataPackTask
import nl.jochembroekhoff.gazebo.standalone.langsapi.GzbsLanguagesProvider
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.resource.IResourceService
import org.metaborg.util.log.LoggerUtils
import org.metaborg.util.time.Timer
import picocli.CommandLine.*
import java.io.File
import java.util.ServiceLoader
import java.util.concurrent.Callable

inline fun <T, U : T, V : T> U.runIf(cond: Boolean, block: U.() -> V): T {
    return run {
        if (cond) {
            block()
        } else {
            this
        }
    }
}

@Command(
    name = "gazebo",
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
        names = ["--compress"],
        description = ["Compress the output into a single .zip"],
    )
    var compress: Boolean = false

    @Option(
        names = ["--language-archive"],
        arity = "*",
        hidden = true,
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
            ServiceLoader
                .load(GzbsLanguagesProvider::class.java)
                .asSequence()
                .flatMap { it.provideLibraries(resourceService) }
                .toList()
        }
    }

    private fun createRunnerConfiguration(): GazeboRunnerConfiguration {
        return GazeboRunnerConfiguration(
            root,
            logToStdout = true,
        )
    }

    override fun call(): Int {
        val timer = Timer(true)

        val runnerConfig = createRunnerConfiguration()

        val ok = when (internalAction) {
            InternalAction.DEFAULT -> {
                GazeboSpoofaxFactory.createGazeboSpoofax(
                    GazeboProjectConfigServiceConfig(
                        libs = setOf("std.mcje.gzb", "std.mcje.gzbc", "std.mcje.llmc"),
                        languageArchiveProvider = ::languageArchiveProvider,
                    )
                ).use { spoofax ->
                    val packTaskChain = EmitDataPackTask(EmitDataPackTask.PackFormat.VERSION_9)
                        .runIf(compress) {
                            chain { dpLoc ->
                                CompressDataPackTask(dpLoc)
                            }
                        }
                    val ok = GazeboRunner(runnerConfig)
                        .withOverlayTask(packTaskChain)
                        .run(spoofax)
                    // TODO: get error messages and print them here, instead of letting GazeboRunner do it

                    packTaskChain.result()?.let {
                        logger.info("Data pack output can be found at {}", it)
                    }

                    ok
                }
            }
            InternalAction.STDLIB -> {
                GazeboSpoofaxFactory.createGazeboSpoofax(
                    GazeboProjectConfigServiceConfig(
                        languages = setOf(GazeboLang.GZB, GazeboLang.GZBC, GazeboLang.LLMC),
                        extensions = setOf(GazeboExt.GZB2GZBC, GazeboExt.GZBC2LLMC),
                        languageArchiveProvider = ::languageArchiveProvider,
                    )
                ).use { spoofax ->
                    GazeboRunner(runnerConfig)
                        .withOverlayTask(EmitStxLib(GazeboLang.GZB))
                        .withOverlayTask(EmitStxLib(GazeboLang.GZBC))
                        .run(spoofax)
                    // TODO: get error messages and print them here, instead of letting GazeboRunner do it
                }
            }
        }

        val sec = timer.stop() / 1e9
        logger.info("Finished after {} seconds", sec)

        return if (ok) 0 else 1
    }
}
