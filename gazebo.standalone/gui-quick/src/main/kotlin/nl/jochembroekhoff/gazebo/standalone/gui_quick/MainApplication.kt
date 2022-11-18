package nl.jochembroekhoff.gazebo.standalone.gui_quick

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import nl.jochembroekhoff.gazebo.standalone.lib.GazeboSpoofaxFactory
import nl.jochembroekhoff.gazebo.standalone.lib.project.GazeboProjectConfigServiceConfig
import org.metaborg.spoofax.core.Spoofax

class MainApplication : Application() {

    private lateinit var spoofax: Spoofax

    override fun init() {
        super.init()
        spoofax = GazeboSpoofaxFactory.createGazeboSpoofax(
            GazeboProjectConfigServiceConfig(
                libs = setOf("std.mcje.gzb", "std.mcje.gzbc", "std.mcje.llmc"),
            ) { resourceService ->
                listOf(
                    "../lang/lang.gazebo/target/lang.gazebo-0.1.0-SNAPSHOT.spoofax-language",
                    "../lang/lang.gazebo-core/target/lang.gazebo-core-0.1.0-SNAPSHOT.spoofax-language",
                    "../lang/lang.llmc/target/lang.llmc-0.1.0-SNAPSHOT.spoofax-language",
                    "../ext/ext.gzb2gzbc/target/ext.gzb2gzbc-0.1.0-SNAPSHOT.spoofax-language",
                    "../ext/ext.gzbc2llmc/target/ext.gzbc2llmc-0.1.0-SNAPSHOT.spoofax-language",
                    "../ext/ext.llmc2mcje/target/ext.llmc2mcje-0.1.0-SNAPSHOT.spoofax-language",
                    "../tools/lib.std.mcje.gzb-1.18.2+0-0.1.0-SNAPSHOT.spoofax-language",
                    "../tools/lib.std.mcje.gzbc-1.18.2+0-0.1.0-SNAPSHOT.spoofax-language",
                    "../tools/lib.std.mcje.llmc-1.18.2+0-0.1.0-SNAPSHOT.spoofax-language"
                ).map(resourceService::resolve)
            }
        )
    }

    override fun start(primaryStage: Stage) {
        primaryStage.apply {
            title = "Gazebo Quick GUI - Evaluation"
        }

        val (mainContent, mainController) = GuiLoaderUtil.load<MainController>("Main")
        mainController.configure(spoofax, spoofax.resourceService.resolve("/tmp/gzb_project"))

        primaryStage.scene = Scene(mainContent)

        primaryStage.show()
        primaryStage.centerOnScreen()
    }

    override fun stop() {
        super.stop()
        spoofax.close()
    }
}
