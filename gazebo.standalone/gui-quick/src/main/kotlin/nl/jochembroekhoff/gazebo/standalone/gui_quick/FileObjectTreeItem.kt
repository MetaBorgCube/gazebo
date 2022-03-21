package nl.jochembroekhoff.gazebo.standalone.gui_quick

import javafx.collections.ObservableList
import javafx.scene.control.TreeItem
import org.apache.commons.vfs2.FileObject

class FileObjectTreeItem(private val fileObject: FileObject) : TreeItem<FileObject>(fileObject) {

    private var firstTimeChildren = true
    private val isLeaf = lazy(LazyThreadSafetyMode.NONE) { fileObject.isFile }

    override fun getChildren(): ObservableList<TreeItem<FileObject>> {
        if (!firstTimeChildren)
            return super.getChildren()
        firstTimeChildren = false

        return super.getChildren().apply {
            setAll(constructChildren())
        }
    }

    override fun isLeaf(): Boolean {
        return isLeaf.value
    }

    private fun constructChildren(): Collection<TreeItem<FileObject>> {
        return fileObject.children.map {
            FileObjectTreeItem(it)
        }
    }
}
