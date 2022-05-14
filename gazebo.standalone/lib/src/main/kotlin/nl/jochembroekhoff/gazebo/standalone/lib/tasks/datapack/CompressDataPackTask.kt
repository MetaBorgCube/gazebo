package nl.jochembroekhoff.gazebo.standalone.lib.tasks.datapack

import nl.jochembroekhoff.gazebo.standalone.lib.tasks.OverlayTask
import org.apache.commons.io.IOUtils
import org.apache.commons.vfs2.AllFileSelector
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.build.CommonPaths
import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput
import org.metaborg.spoofax.core.shell.CLIUtils
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CompressDataPackTask(private val uncompressedRoot: FileObject?) : OverlayTask<FileObject?>("compress-data-pack") {

    private var result: FileObject? = null

    override fun run(spoofax: Spoofax, cli: CLIUtils, project: IProject, output: ISpoofaxBuildOutput) {
        val sourceDir = uncompressedRoot ?: return
        val paths = CommonPaths(project.location())

        val destZipFile = paths.targetDir().resolveFile("data-pack.zip")
        result = destZipFile

        ZipOutputStream(destZipFile.content.outputStream).use { zip ->
            zip.setComment("Gazebo Compiler ${javaClass.`package`.implementationVersion ?: "dev"}")
            sourceDir.findFiles(AllFileSelector()).forEach { file ->
                if (!file.isFile) return@forEach
                zip.putNextEntry(ZipEntry(uncompressedRoot.name.getRelativeName(file.name)))
                file.content.inputStream.use {
                    IOUtils.copy(it, zip)
                }
            }
        }
    }

    override fun result(): FileObject? {
        return result
    }
}
