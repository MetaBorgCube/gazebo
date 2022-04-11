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

class EmitDataPackTask(private val format: PackFormat) : AdditionalTask<FileObject?>("emit-data-pack") {

    enum class PackFormat(val intValue: Int) {
        /** MC:JE 1.13-1.14 */
        VERSION_4(4),
        /** MC:JE 1.15-1.16.1 */
        VERSION_5(5),
        /** MC:JE 1.16.2-1.16.5 */
        VERSION_6(6),
        /** MC:JE 1.17 */
        VERSION_7(7),
        /** MC:JE 1.18-1.18.1 */
        VERSION_8(8),
        /** MC:JE 1.18.2 */
        VERSION_9(9),
        /** MC:JE 1.19 */
        VERSION_10(10),
    }

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
            IOUtils.write(
                """{"pack":{"pack_format":${format.intValue},"description":"Gazebo Data Pack Output"}}""",
                it,
                Charsets.UTF_8
            )
        }
    }

    override fun result(): FileObject? {
        return result
    }
}
