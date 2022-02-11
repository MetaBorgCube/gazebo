package nl.jochembroekhoff.gazebo.standalone.lib.tasks

import org.apache.commons.vfs2.FileObject
import org.metaborg.core.build.CommonPaths
import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput
import org.metaborg.spoofax.core.shell.CLIUtils
import org.metaborg.util.resource.AntPatternFileSelector
import java.util.*

class EmitDataPack : AdditionalTask<FileObject?>("emit-data-pack") {

    private var result: FileObject? = null

    override fun run(spoofax: Spoofax, cli: CLIUtils, project: IProject, output: ISpoofaxBuildOutput) {
        val paths = CommonPaths(project.location())

        val packDir = paths.targetDir().resolveFile("data-pack")
        packDir.createFolder()
        result = packDir

        val mcFunctionDir = paths.srcGenDir().resolveFile("gzb-fin-interm")
        Arrays.stream(mcFunctionDir.findFiles(AntPatternFileSelector("*/*/**/*.mcfunction")))
            .map { mcFunction ->
                mcFunction.name.getRelativeName(mcFunctionDir.name)
            }
    }

    override fun result(): FileObject? {
        return result
    }
}
