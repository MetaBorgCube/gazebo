package nl.jochembroekhoff.gazebo.standalone.gui_quick

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.*
import nl.jochembroekhoff.gazebo.standalone.lib.GazeboSpoofaxFactory
import nl.jochembroekhoff.gazebo.standalone.lib.project.GazeboProjectConfigServiceConfig
import nl.jochembroekhoff.gazebo.standalone.lib.runner.GazeboRunner
import nl.jochembroekhoff.gazebo.standalone.lib.runner.GazeboRunnerConfiguration
import org.apache.commons.io.FileUtils
import org.apache.commons.vfs2.FileObject
import org.metaborg.spoofax.core.Spoofax
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

class MainController {

    @FXML
    private lateinit var contentGZB: Tab

    @FXML
    private lateinit var contentGZBC: Tab

    @FXML
    private lateinit var contentLLMC: Tab

    @FXML
    private lateinit var compileButton: Button

    @FXML
    private lateinit var compileStatus: Label

    @FXML
    private lateinit var compileProgress: ProgressBar

    @FXML
    private lateinit var outputArea: TextArea

    private lateinit var spoofax: Spoofax
    private lateinit var gzbSourceEditor: EditorController
    private lateinit var outputViewers: Set<EditorController>

    fun configure(spoofax: Spoofax, root: FileObject) {
        this.spoofax = spoofax

        gzbSourceEditor = GuiLoaderUtil.loadInto<EditorController>(contentGZB, "Editor").apply {
            configure(root.resolveFile("data"), true)
        }
        outputViewers = setOf(
            GuiLoaderUtil.loadInto<EditorController>(contentGZBC, "Editor").apply {
                configure(root.resolveFile("src-gen/gzb-interm"), false)
            },
            GuiLoaderUtil.loadInto<EditorController>(contentLLMC, "Editor").apply {
                configure(root.resolveFile("src-gen/llmc-interm"), false)
            }
        )
    }

    @FXML
    private fun compile() {
        prepareControls()
        gzbSourceEditor.save()

        thread(start = true) {

            try {
                FileUtils.deleteDirectory(File("/tmp/gzb_project/target"))
            } catch (ignored: IOException) {
            }
            try {
                FileUtils.deleteDirectory(File("/tmp/gzb_project/src-gen"))
            } catch (ignored: IOException) {
            }

            val runnerConfig = GazeboRunnerConfiguration(
                File("/tmp/gzb_project"),
                { resourceService ->
                    listOf(
                        "../lang/lang.gazebo/target/lang.gazebo-0.1.0-SNAPSHOT.spoofax-language",
                        "../lang/lang.gazebo-core/target/lang.gazebo-core-0.1.0-SNAPSHOT.spoofax-language",
                        "../lang/lang.llmc/target/lang.llmc-0.1.0-SNAPSHOT.spoofax-language",
                        "../ext/ext.gzb2gzbc/target/ext.gzb2gzbc-0.1.0-SNAPSHOT.spoofax-language",
                        "../ext/ext.gzbc2llmc/target/ext.gzbc2llmc-0.1.0-SNAPSHOT.spoofax-language",
                        "../ext/ext.llmc2mcje/target/ext.llmc2mcje-0.1.0-SNAPSHOT.spoofax-language",
                        "../tools/lib.std.mcje.gzb-1.18.1+0-0.1.0-SNAPSHOT.spoofax-language",
                        "../tools/lib.std.mcje.gzbc-1.18.1+0-0.1.0-SNAPSHOT.spoofax-language",
                        "../tools/lib.std.mcje.llmc-1.18.1+0-0.1.0-SNAPSHOT.spoofax-language"
                    ).map(resourceService::resolve)
                },
                logAux = TextAreaMessagePrinter(outputArea)
            )

            val success = GazeboRunner(runnerConfig)
                .run(
                    spoofax,
                    ProgressBarProgressImpl(compileProgress, compileStatus)
                )

            Platform.runLater {
                finishControls(success)
            }
        }
    }

    private fun prepareControls() {
        compileStatus.text = ""
        compileStatus.isVisible = true
        compileProgress.progress = -1.0
        compileProgress.isVisible = true
        outputArea.clear()
        compileButton.isDisable = true
    }

    private fun finishControls(compilationSuccess: Boolean) {
        compileStatus.isVisible = false
        compileProgress.isVisible = false
        compileButton.isDisable = false
        outputViewers.forEach {
            it.refresh()
        }

        outputArea.appendText("==========\n")
        if (compilationSuccess) {
            outputArea.appendText("> compilation SUCCEEDED!\n")
        } else {
            outputArea.appendText("> compilation FAILED!\n")
            outputArea.appendText("  (see messages above, make sure input files are not empty)\n")
        }
    }

}
