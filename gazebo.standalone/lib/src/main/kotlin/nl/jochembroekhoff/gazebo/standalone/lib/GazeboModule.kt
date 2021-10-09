package nl.jochembroekhoff.gazebo.standalone.lib

import com.google.inject.Singleton
import org.metaborg.core.config.IProjectConfigService
import org.metaborg.core.editor.IEditorRegistry
import org.metaborg.core.editor.NullEditorRegistry
import org.metaborg.spoofax.core.SpoofaxModule
import org.metaborg.spoofax.core.config.ISpoofaxProjectConfigService
import javax.inject.Provider

class GazeboModule(private val projectConfigServiceConfig: GazeboProjectConfigServiceConfig) : SpoofaxModule() {

    private fun projectServiceFactory(): GazeboProjectConfigService {
        return GazeboProjectConfigService(projectConfigServiceConfig)
    }

    override fun bindProjectConfig() {
        bind(IProjectConfigService::class.java).toProvider(Provider { projectServiceFactory() })
        bind(ISpoofaxProjectConfigService::class.java).toProvider(Provider { projectServiceFactory() })

        // not bound: IProjectConfigWriter, IProjectConfigBuilder
    }

    override fun bindEditor() {
        bind(IEditorRegistry::class.java).to(NullEditorRegistry::class.java).`in`(Singleton::class.java)
    }
}
