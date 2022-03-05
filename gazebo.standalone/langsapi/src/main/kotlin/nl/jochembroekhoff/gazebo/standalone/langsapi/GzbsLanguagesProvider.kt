package nl.jochembroekhoff.gazebo.standalone.langsapi

import org.apache.commons.vfs2.FileObject
import org.metaborg.core.resource.IResourceService

interface GzbsLanguagesProvider {
    fun provideLibraries(resourceService: IResourceService): List<FileObject>
}
