package nl.jochembroekhoff.gazebo.standalone.gui_quick

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.*
import nl.jochembroekhoff.gazebo.standalone.lib.runner.GazeboRunner
import nl.jochembroekhoff.gazebo.standalone.lib.runner.GazeboRunnerConfiguration
import nl.jochembroekhoff.gazebo.standalone.lib.tasks.datapack.EmitDataPackTask
import org.apache.commons.io.FileUtils
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.shell.CLIUtils
import org.spoofax.terms.StrategoString
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

class MainController {

    companion object {
        val PROJECT_ROOT = File("/tmp/gzb_project")
    }

    @FXML
    private lateinit var contentGZB: Tab

    @FXML
    private lateinit var contentGZBC: Tab

    @FXML
    private lateinit var contentLLMC: Tab

    @FXML
    private lateinit var contentPack: Tab

    @FXML
    private lateinit var compileButton: Button

    @FXML
    private lateinit var compileStatus: Label

    @FXML
    private lateinit var compileProgress: ProgressBar

    @FXML
    private lateinit var outputArea: TextArea

    private lateinit var spoofax: Spoofax
    private lateinit var ppProject: IProject
    private lateinit var gzbSourceEditor: EditorController
    private lateinit var outputViewers: Set<EditorController>

    fun configure(spoofax: Spoofax, root: FileObject) {
        this.spoofax = spoofax
        this.ppProject = CLIUtils(spoofax).getOrCreateProject(spoofax.resolve(PROJECT_ROOT))

        gzbSourceEditor = GuiLoaderUtil.loadInto<EditorController>(contentGZB, "Editor").apply {
            configure(root.resolveFile("data"), true)
        }
        outputViewers = setOf(
            GuiLoaderUtil.loadInto<EditorController>(contentGZBC, "Editor").apply {
                configure(root.resolveFile("src-gen/gzb-interm"), false, createPP("gazebo-core"))
            },
            GuiLoaderUtil.loadInto<EditorController>(contentLLMC, "Editor").apply {
                configure(root.resolveFile("src-gen/llmc-interm"), false, createPP("llmc"))
            },
            GuiLoaderUtil.loadInto<EditorController>(contentPack, "Editor").apply {
                configure(root.resolveFile("target/data-pack/data"), false)
            },
        )
    }

    private fun createPP(langName: String): ((String) -> String) {
        val langImpl = spoofax.languageService.getLanguage(langName)?.activeImpl() ?: return { it }
        return { aTermString ->
            spoofax.contextService.getTemporary(null, ppProject, langImpl).use { ctx ->
                try {
                    val term = spoofax.termFactory.parseFromString(aTermString)
                    val result = spoofax.strategoCommon.invoke(
                        langImpl,
                        ctx,
                        term,
                        "pp-$langName-string",
                        listOf()
                    )
                    (result as? StrategoString)?.stringValue() ?: aTermString
                } catch (e: Exception) {
                    e.printStackTrace()
                    aTermString
                }
            }
        }
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
                PROJECT_ROOT,
                logAux = TextAreaMessagePrinter(outputArea),
            )

            val success = GazeboRunner(runnerConfig)
                .withAdditionalTask(EmitDataPackTask(EmitDataPackTask.PackFormat.VERSION_8))
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
