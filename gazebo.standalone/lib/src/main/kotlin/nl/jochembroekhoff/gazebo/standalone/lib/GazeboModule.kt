package nl.jochembroekhoff.gazebo.standalone.lib

import com.google.inject.Singleton
import org.metaborg.core.config.IProjectConfigService
import org.metaborg.core.editor.IEditorRegistry
import org.metaborg.core.editor.NullEditorRegistry
import org.metaborg.spoofax.core.SpoofaxModule
import org.metaborg.spoofax.core.config.ISpoofaxProjectConfigService

class GazeboModule : SpoofaxModule() {

    override fun bindProjectConfig() {
        bind(GazeboProjectConfigService::class.java).`in`(Singleton::class.java)
        bind(IProjectConfigService::class.java).to(GazeboProjectConfigService::class.java)
        bind(ISpoofaxProjectConfigService::class.java).to(GazeboProjectConfigService::class.java)

        // not bound: IProjectConfigWriter, IProjectConfigBuilder
    }

    override fun bindEditor() {
        bind(IEditorRegistry::class.java).to(NullEditorRegistry::class.java).`in`(Singleton::class.java)
    }
}
