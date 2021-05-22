package nl.jochembroekhoff.gazebo.standalone.lib

import org.apache.commons.vfs2.FileObject
import org.metaborg.core.resource.IResourceService
import java.io.File

data class GazeboRunnerConfiguration(
    val root: File,
    val languageArchiveProvider: (resourceService: IResourceService) -> Collection<FileObject>,
)
