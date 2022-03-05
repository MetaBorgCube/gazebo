package nl.jochembroekhoff.gazebo.standalone.defaultlibs

import nl.jochembroekhoff.gazebo.standalone.langsapi.GzbsLanguagesProvider
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.resource.IResourceService

class DefaultLanguagesProvider : GzbsLanguagesProvider {
    override fun provideLibraries(resourceService: IResourceService): List<FileObject> {
        val clazz = DefaultLanguagesProvider::class.java
        return resourceService
            .resolve(clazz.getResource("/gzbs-builtin-langs.txt")!!.toURI())
            .content
            .getString(Charsets.UTF_8)
            .lineSequence()
            .map { clazz.getResource("/$it") }
            .filterNotNull()
            .map { it.toURI() }
            .map { resourceService.resolve(it) }
            .toList()
    }
}
