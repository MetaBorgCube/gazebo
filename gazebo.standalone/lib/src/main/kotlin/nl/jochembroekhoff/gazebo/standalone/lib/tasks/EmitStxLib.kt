package nl.jochembroekhoff.gazebo.standalone.lib.tasks

import nl.jochembroekhoff.gazebo.standalone.lib.GazeboLang
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput
import org.metaborg.spoofax.core.shell.CLIUtils
import org.metaborg.util.log.LoggerUtils
import org.metaborg.util.time.Timer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class EmitStxLib(targetLang: GazeboLang) : AdditionalTask<Future<FileObject>>("emit-stxlib") {

    companion object {
        // TODO: put this somewhere else
        private val langNameMapping = mapOf(
            GazeboLang.GZB to "gazebo",
            GazeboLang.GZBC to "gazebo-core",
            GazeboLang.MCAM to "mcam",
        )
    }

    private val logger = LoggerUtils.logger(javaClass)

    private val resultFuture = CompletableFuture<FileObject>()
    private val targetLangName = langNameMapping[targetLang]!!

    override fun run(spoofax: Spoofax, cli: CLIUtils, project: IProject, output: ISpoofaxBuildOutput) {
        val langImpl = cli.getLanguage(targetLangName)
        val stxlibGoal = cli.getNamedTransformAction("Make project library", langImpl)

        val stxlibTimer = Timer(true)
        val firstAnalysis = output.analysisResults().first()
        val res = cli.transform(firstAnalysis, stxlibGoal, firstAnalysis.context())
        logger.info("created stxlib in {} seconds", stxlibTimer.stop() / 1e9)

        val outFile = taskDefaultOutput(project).resolveFile("$targetLangName.stxlib")
        outFile.content.outputStream.writer().use {
            res.writeAsString(it)
        }
        resultFuture.complete(outFile)
    }

    override fun result(): Future<FileObject> {
        return resultFuture
    }
}
