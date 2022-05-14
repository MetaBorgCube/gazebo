package nl.jochembroekhoff.gazebo.standalone.lib.tasks.datapack

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import nl.jochembroekhoff.gazebo.standalone.lib.NSID
import nl.jochembroekhoff.gazebo.standalone.lib.imp.IMPHandlesAnnotation
import nl.jochembroekhoff.gazebo.standalone.lib.imp.IMPParser
import nl.jochembroekhoff.gazebo.standalone.lib.tasks.OverlayTask
import org.apache.commons.io.IOUtils
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.build.CommonPaths
import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput
import org.metaborg.spoofax.core.shell.CLIUtils
import org.metaborg.util.log.LoggerUtils
import org.metaborg.util.resource.AntPatternFileSelector

class EmitDataPackTask(private val format: PackFormat) : OverlayTask<FileObject?>("emit-data-pack") {

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

    private val logger = LoggerUtils.logger(javaClass)

    private var result: FileObject? = null

    override fun run(spoofax: Spoofax, cli: CLIUtils, project: IProject, output: ISpoofaxBuildOutput) {
        val paths = CommonPaths(project.location())

        val packDir = paths.targetDir().resolveFile("data-pack")
        packDir.deleteAll()
        packDir.createFolder()
        result = packDir

        val packDataDir = packDir.resolveFile("data")
        copyFunctions(paths, packDataDir)
        writeTags(packDataDir, collectTags(packDataDir))

        packDir.resolveFile("pack.mcmeta").content.outputStream.use {
            IOUtils.write(
                """{"pack":{"pack_format":${format.intValue},"description":"Gazebo Data Pack Output"}}""",
                it,
                Charsets.UTF_8
            )
        }
    }

    private fun copyFunctions(paths: CommonPaths, packDataDir: FileObject) {
        val mcFunctionDir = paths.srcGenDir().resolveFile("gzb-fin-interm")
        mcFunctionDir.children.asSequence()
            .filter { it.isFolder }
            .forEach { namespaceDir ->
                val namespace = mcFunctionDir.name.getRelativeName(namespaceDir.name)
                val namespacePackDir = packDataDir.resolveFile(namespace)

                // copy all functions into the data pack
                val functionsPackDir = namespacePackDir.resolveFile("functions")
                functionsPackDir.copyFrom(namespaceDir, AntPatternFileSelector("**/*.mcfunction"))
            }
    }

    private fun collectTags(packDataDir: FileObject): Map<NSID, Set<NSID>> {
        return buildMap<NSID, MutableSet<NSID>> {
            packDataDir.findFiles(AntPatternFileSelector("*/functions/**/*.mcfunction")).forEach {
                IMPParser.parseSpecFromMcfunction(it.content)?.let { spec ->
                    spec.annotations.asSequence()
                        .filterIsInstance<IMPHandlesAnnotation>()
                        .flatMap { anno -> anno.handles }
                        .forEach { tag ->
                            logger.info("Register {} to tag {}", spec.name.toMinecraftString(), tag.toMinecraftString())
                            computeIfAbsent(tag) { linkedSetOf() }.add(spec.name)
                        }
                }
            }
        }
    }

    private fun writeTags(packDataDir: FileObject, tags: Map<NSID, Set<NSID>>) {
        val mapper = ObjectMapper()
        val writer = mapper.writer(DefaultPrettyPrinter().apply {
            indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
        })
        tags.forEach { (tag, values) ->
            // Create content object
            val content = mapper.nodeFactory.objectNode().apply {
                set<ObjectNode>("tags", mapper.nodeFactory.arrayNode().apply {
                    values.forEach { add(it.toMinecraftString()) }
                })
            }

            // Write to JSON file
            packDataDir
                .resolveFile(tag.namespace)
                .resolveFile("tags")
                .resolveFile("functions")
                .resolveFile(tag.name.joinToString("/") + ".json")
                .content.outputStream.use {
                    writer.writeValue(it, content)
                }
        }
    }

    override fun result(): FileObject? {
        return result
    }
}
