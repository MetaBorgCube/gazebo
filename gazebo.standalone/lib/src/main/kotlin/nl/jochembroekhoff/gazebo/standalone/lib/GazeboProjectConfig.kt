package nl.jochembroekhoff.gazebo.standalone.lib

import mb.nabl2.config.NaBL2Config
import mb.statix.spoofax.IStatixProjectConfig
import mb.statix.spoofax.StatixProjectConfig
import org.metaborg.core.MetaborgConstants
import org.metaborg.core.config.ISourceConfig
import org.metaborg.core.config.LangSource
import org.metaborg.core.language.LanguageIdentifier
import org.metaborg.spoofax.core.config.ISpoofaxProjectConfig

class GazeboProjectConfig(
    private val languages: Set<GazeboLang>,
    private val extensions: Set<GazeboExt>,
    private val libs: Set<String>,
) : ISpoofaxProjectConfig {

    companion object {

        private val componentMapping = mapOf(
            GazeboLang.GZB to "lang.gazebo",
            GazeboLang.GZBC to "lang.gazebo-core",
            GazeboLang.LLMC to "lang.llmc",
            GazeboExt.GZB2GZBC to "ext.gzb2gzbc",
            GazeboExt.GZBC2LLMC to "ext.gzbc2llmc",
            GazeboExt.LLMC2MCJE to "ext.llmc2mcje",
        )

        private fun gazeboProjectIdentifier(component: String): String {
            return "nl.jochembroekhoff.gazebo:${component}:0.1.0-SNAPSHOT"
        }

        private fun gazeboProjectIdentifier(component: GazeboLang): String {
            return gazeboProjectIdentifier(componentMapping[component]!!)
        }

        private fun gazeboProjectIdentifier(extension: GazeboExt): String {
            return gazeboProjectIdentifier(componentMapping[extension]!!)
        }
    }

    override fun metaborgVersion(): String {
        return MetaborgConstants.METABORG_VERSION
    }

    override fun sources(): Collection<ISourceConfig> {
        return listOf(
            LangSource("gazebo", "./data"),
            LangSource("gazebo-core", "./src-gen/gzb-interm"),
            LangSource("llmc", "./src-gen/llmc-interm"),
        )
    }

    override fun compileDeps(): Collection<LanguageIdentifier> {
        return (languages.map(::gazeboProjectIdentifier) + extensions.map(::gazeboProjectIdentifier))
            .map(LanguageIdentifier::parse)
    }

    override fun sourceDeps(): Collection<LanguageIdentifier> {
        return (languages.map(::gazeboProjectIdentifier) + libs.map { "lib.$it" }.map(::gazeboProjectIdentifier))
            .map(LanguageIdentifier::parse)
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
