package nl.jochembroekhoff.gazebo.standalone.lib

import com.google.inject.util.Modules
import org.metaborg.spoofax.core.Spoofax

object GazeboSpoofaxFactory {
    fun createGazeboSpoofax(): Spoofax {
        return Spoofax(EmptySpoofaxModule(), Modules.override(GazeboModule()).with(GazeboOverridesModule()))
    }
}
