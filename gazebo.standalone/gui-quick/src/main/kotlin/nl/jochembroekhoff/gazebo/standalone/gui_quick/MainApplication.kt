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
                libs = setOf("std.mcje.gzb", "std.mcje.gzbc", "std.mcje.llmc")
            )
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
