package nl.jochembroekhoff.gazebo.standalone.lib

import com.google.inject.AbstractModule
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService

class GazeboOverridesModule : AbstractModule() {
    override fun configure() {
        bind(ISpoofaxSyntaxService::class.java).to(ATermSpeedSyntaxService::class.java)
    }
}
