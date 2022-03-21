package nl.jochembroekhoff.gazebo.standalone.gui_quick

import javafx.fxml.FXML
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeView
import org.apache.commons.io.IOUtils
import org.apache.commons.vfs2.FileObject
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory

class EditorController {

    @FXML
    private lateinit var fileTree: TreeView<FileObject>
    @FXML
    private lateinit var fileContent: CodeArea

    private lateinit var root: FileObject
    private lateinit var pp: ((String) -> String)
    private var currentFile: FileObject? = null

    @FXML
    private fun initialize() {
        fileTree.isShowRoot = false

        fileTree.setCellFactory {
            object : TreeCell<FileObject>() {

                init {
                    setOnMouseClicked {
                        edit(item)
                    }
                }

                override fun updateItem(item: FileObject?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        text = null
                    } else {
                        text = item.name.baseName
                    }
                }
            }
        }

        fileContent.paragraphGraphicFactory = LineNumberFactory.get(fileContent)
    }

    fun configure(root: FileObject, editable: Boolean, pp: ((String) -> String)? = null) {
        this.root = root
        this.pp = pp ?: { it }
        fileContent.isEditable = editable

        refresh()
    }

    fun refresh() {
        // Make sure the folder exists
        root.createFolder()

        // Clear editor contents completely
        currentFile = null
        fileContent.clear()

        fileTree.root = FileObjectTreeItem(root)
        fileTree.root.isExpanded = true
    }

    private fun edit(item: FileObject?) {
        if (item == null || !item.isFile) return

        save()
        currentFile = item
        load()
    }

    fun save() {
        currentFile?.let {
            it.content.outputStream.use { os ->
                IOUtils.write(fileContent.text, os, Charsets.UTF_8)
            }
        }
    }

    fun load() {
        val rawContent = currentFile?.content?.getString(Charsets.UTF_8) ?: ""
        fileContent.replaceText(pp(rawContent))
    }
}
