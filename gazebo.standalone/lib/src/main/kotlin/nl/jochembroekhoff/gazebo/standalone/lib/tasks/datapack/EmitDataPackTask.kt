package nl.jochembroekhoff.gazebo.standalone.lib.tasks.datapack

import nl.jochembroekhoff.gazebo.standalone.lib.tasks.AdditionalTask
import org.apache.commons.io.IOUtils
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.build.CommonPaths
import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput
import org.metaborg.spoofax.core.shell.CLIUtils
import org.metaborg.util.resource.AntPatternFileSelector

class EmitDataPackTask : AdditionalTask<FileObject?>("emit-data-pack") {

    private var result: FileObject? = null

    override fun run(spoofax: Spoofax, cli: CLIUtils, project: IProject, output: ISpoofaxBuildOutput) {
        val paths = CommonPaths(project.location())

        val packDir = paths.targetDir().resolveFile("data-pack")
        packDir.deleteAll()
        packDir.createFolder()
        result = packDir

        val mcFunctionDir = paths.srcGenDir().resolveFile("gzb-fin-interm")
        mcFunctionDir.children.asSequence()
            .filter { it.isFolder }
            .forEach { namespaceDir ->
                val namespace = mcFunctionDir.name.getRelativeName(namespaceDir.name)
                val namespacePackDir = packDir.resolveFile("data").resolveFile(namespace)

                // copy all functions into the data pack
                val functionsPackDir = namespacePackDir.resolveFile("functions")
                functionsPackDir.copyFrom(namespaceDir, AntPatternFileSelector("**/*.mcfunction"))
            }

        // TODO: write tag files

        packDir.resolveFile("pack.mcmeta").content.outputStream.use {
            IOUtils.write("""{"pack":{"pack_format":8},"description":"Gazebo Data Pack Output"}""", it, Charsets.UTF_8)
        }
    }

    override fun result(): FileObject? {
        return result
    }
}
