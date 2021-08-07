package nl.jochembroekhoff.gazebo.standalone.lib

import mb.nabl2.config.NaBL2Config
import mb.statix.spoofax.IStatixProjectConfig
import mb.statix.spoofax.StatixProjectConfig
import org.metaborg.core.MetaborgConstants
import org.metaborg.core.config.ISourceConfig
import org.metaborg.core.config.LangSource
import org.metaborg.core.language.LanguageIdentifier
import org.metaborg.spoofax.core.config.ISpoofaxProjectConfig

class GazeboProjectConfig : ISpoofaxProjectConfig {

    companion object {
        private fun gazeboProjectIdentifier(component: String): String {
            return "nl.jochembroekhoff.gazebo:${component}:0.1.0-SNAPSHOT"
        }
    }

    override fun metaborgVersion(): String {
        return MetaborgConstants.METABORG_VERSION
    }

    override fun sources(): Collection<ISourceConfig> {
        return listOf(
            LangSource("gazebo", "./data"),
            LangSource("gazebo-core", "./src-gen/gzb-interm"),
            LangSource("mcam", "./src-gen/gzb-interm"),
        )
    }

    override fun compileDeps(): Collection<LanguageIdentifier> {
        return listOf(
            LanguageIdentifier.parse(gazeboProjectIdentifier("lang.gazebo")),
            LanguageIdentifier.parse(gazeboProjectIdentifier("lang.gazebo-core")),
            LanguageIdentifier.parse(gazeboProjectIdentifier("lang.mcam")),
            LanguageIdentifier.parse(gazeboProjectIdentifier("ext.gzb2gzbc")),
            LanguageIdentifier.parse(gazeboProjectIdentifier("ext.gzbc2mcam")),
            LanguageIdentifier.parse(gazeboProjectIdentifier("ext.mcam2mcje")),
        )
    }

    override fun sourceDeps(): Collection<LanguageIdentifier> {
        return listOf(
            LanguageIdentifier.parse(gazeboProjectIdentifier("lang.gazebo")),
            LanguageIdentifier.parse(gazeboProjectIdentifier("lang.gazebo-core")),
            LanguageIdentifier.parse(gazeboProjectIdentifier("lang.mcam")),
        )
    }

    override fun javaDeps(): Collection<LanguageIdentifier> {
        return emptyList()
    }

    override fun nabl2Config(): NaBL2Config {
        return NaBL2Config.DEFAULT
    }

    override fun statixConfig(): IStatixProjectConfig {
        // Gazebo language will be automatically configured as parallel because it is defined to be so in its own
        //  metaborg.yaml file
        return StatixProjectConfig(null, 5, 3)
    }
}
