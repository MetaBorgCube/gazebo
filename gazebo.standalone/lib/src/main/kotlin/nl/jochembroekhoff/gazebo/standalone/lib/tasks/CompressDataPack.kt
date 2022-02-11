package nl.jochembroekhoff.gazebo.standalone.lib.tasks

import org.apache.commons.vfs2.FileObject
import org.metaborg.core.build.CommonPaths
import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput
import org.metaborg.spoofax.core.shell.CLIUtils

class CompressDataPack(private val uncompressedRoot: FileObject?) : AdditionalTask<FileObject?>("compress-data-pack") {

    private var result: FileObject? = null

    override fun run(spoofax: Spoofax, cli: CLIUtils, project: IProject, output: ISpoofaxBuildOutput) {
        val paths = CommonPaths(project.location())

        val packZipFile = paths.targetDir().resolveFile("data-pack.zip")
        result = packZipFile
    }

    override fun result(): FileObject? {
        return result
    }
}
