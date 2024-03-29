package nl.jochembroekhoff.gazebo.standalone.lib.project

import nl.jochembroekhoff.gazebo.standalone.lib.constants.GazeboExt
import nl.jochembroekhoff.gazebo.standalone.lib.constants.GazeboLang
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.config.ConfigRequest
import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.config.ISpoofaxProjectConfig
import org.metaborg.spoofax.core.config.ISpoofaxProjectConfigService

class GazeboProjectConfigService(private val config: GazeboProjectConfigServiceConfig) : ISpoofaxProjectConfigService {
    override fun available(rootFolder: FileObject?): Boolean {
        // there is always a configuration available
        return true
    }

    override fun get(rootFolder: FileObject?): ConfigRequest<ISpoofaxProjectConfig> {
        return ConfigRequest(defaultConfig(rootFolder))
    }

    override fun get(project: IProject?): ISpoofaxProjectConfig? {
        val conf = project?.config()
        return if (conf is GazeboProjectConfig) conf else null
    }

    override fun defaultConfig(rootFolder: FileObject?): GazeboProjectConfig {
        return GazeboProjectConfig(
            config.languages ?: GazeboLang.values().toSet(),
            config.extensions ?: GazeboExt.values().toSet(),
            config.libs ?: setOf(),
        )
    }
}
