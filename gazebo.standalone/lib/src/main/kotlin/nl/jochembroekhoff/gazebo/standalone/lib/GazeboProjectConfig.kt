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
    override fun metaborgVersion(): String {
        return MetaborgConstants.METABORG_VERSION
    }

    override fun sources(): Collection<ISourceConfig> {
        return listOf(LangSource("gazebo", "./data"))
    }

    override fun compileDeps(): Collection<LanguageIdentifier> {
        return listOf(
            LanguageIdentifier.parse("nl.jochembroekhoff:gazebo.lang:0.1.0-SNAPSHOT")
        )
    }

    override fun sourceDeps(): Collection<LanguageIdentifier> {
        return listOf(
            LanguageIdentifier.parse("nl.jochembroekhoff:gazebo.lang:0.1.0-SNAPSHOT")
        )
    }

    override fun javaDeps(): Collection<LanguageIdentifier> {
        return emptyList()
    }

    override fun nabl2Config(): NaBL2Config {
        return NaBL2Config.DEFAULT
    }

    override fun statixConfig(): IStatixProjectConfig {
        return StatixProjectConfig(listOf("gazebo"), null, null)
    }
}
