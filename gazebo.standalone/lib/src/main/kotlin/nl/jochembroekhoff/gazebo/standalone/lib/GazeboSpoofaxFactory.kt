package nl.jochembroekhoff.gazebo.standalone.lib

import com.google.inject.util.Modules
import nl.jochembroekhoff.gazebo.standalone.lib.project.GazeboProjectConfigServiceConfig
import org.metaborg.spoofax.core.Spoofax

object GazeboSpoofaxFactory {
    fun createGazeboSpoofax(projectConfigServiceConfig: GazeboProjectConfigServiceConfig): Spoofax {
        return Spoofax(
            EmptySpoofaxModule(),
            Modules.override(GazeboModule(projectConfigServiceConfig))
                .with(GazeboOverridesModule())
        ).also { spoofax ->
            projectConfigServiceConfig.languageArchiveProvider(spoofax.resourceService).forEach { languageArchive ->
                if (languageArchive.isFolder) {
                    spoofax.languageDiscoveryService.languagesFromDirectory(languageArchive)
                } else {
                    spoofax.languageDiscoveryService.languagesFromArchive(languageArchive)
                }
            }
        }
    }
}
