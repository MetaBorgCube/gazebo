package nl.jochembroekhoff.gazebo.standalone.cli

import nl.jochembroekhoff.gazebo.standalone.lib.*
import nl.jochembroekhoff.gazebo.standalone.lib.tasks.EmitStxLib
import nl.jochembroekhoff.gazebo.standalone.lib.tasks.TaskUtil.chain
import nl.jochembroekhoff.gazebo.standalone.lib.tasks.datapack.CompressDataPackTask
import nl.jochembroekhoff.gazebo.standalone.lib.tasks.datapack.EmitDataPackTask
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.resource.IResourceService
import org.metaborg.util.log.LoggerUtils
import org.metaborg.util.time.Timer
import picocli.CommandLine.*
import java.io.File
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
                        libs = setOf("std.mcje.gzb", "std.mcje.gzbc", "std.mcje.llmc")
                    )
                ).use { spoofax ->
                    val packTaskChain = EmitDataPackTask(EmitDataPackTask.PackFormat.VERSION_8)
                        .runIf(compress) {
                            chain { dpLoc ->
                                CompressDataPackTask(dpLoc)
                            }
                        }
                    GazeboRunner(runnerConfig)
                        .withAdditionalTask(packTaskChain)
                        .run(spoofax)
                    // TODO: get error messages and print them here, instead of letting GazeboRunner do it

                    packTaskChain.result()?.let {
                        logger.info("Data pack output can be found at {}", it)
                    }
                }
            }
            InternalAction.STDLIB -> {
                GazeboSpoofaxFactory.createGazeboSpoofax(
                    GazeboProjectConfigServiceConfig(
                        languages = setOf(GazeboLang.GZB, GazeboLang.GZBC, GazeboLang.LLMC),
                        extensions = setOf(GazeboExt.GZB2GZBC, GazeboExt.GZBC2LLMC),
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
