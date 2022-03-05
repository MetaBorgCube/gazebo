package nl.jochembroekhoff.gazebo.standalone.lib.runner

import org.apache.commons.vfs2.FileObject
import org.metaborg.core.messages.IMessagePrinter
import org.metaborg.core.resource.IResourceService
import java.io.File

data class GazeboRunnerConfiguration(
    val root: File,
    val languageArchiveProvider: (resourceService: IResourceService) -> Collection<FileObject>,
    val logToStdout: Boolean = false,
    val logToFile: Boolean = false,
    val logAux: IMessagePrinter? = null,
)
