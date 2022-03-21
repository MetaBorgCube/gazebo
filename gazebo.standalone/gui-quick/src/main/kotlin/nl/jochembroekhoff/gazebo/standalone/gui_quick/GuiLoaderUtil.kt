package nl.jochembroekhoff.gazebo.standalone.gui_quick

import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.ScrollPane
import javafx.scene.control.Tab
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane

object GuiLoaderUtil {
    fun <T> load(name: String): Pair<Parent, T> {
        val res = javaClass.getResource("/nl/jochembroekhoff/gazebo/standalone/gui_quick/$name.fxml")
        val loader = FXMLLoader().apply {
            location = res
            load()
        }
        return Pair(loader.getRoot(), loader.getController<T>())
    }
    fun <T> loadInto(target: Any, name: String): T {
        val (root, controller) = load<T>(name)

        when (target) {
            is Pane -> {
                target.children.clear()
                if (target is AnchorPane) {
                    AnchorPane.setTopAnchor(root, 0.0)
                    AnchorPane.setRightAnchor(root, 0.0)
                    AnchorPane.setBottomAnchor(root, 0.0)
                    AnchorPane.setLeftAnchor(root, 0.0)
                }
                target.children.add(root)
            }
            is ScrollPane -> {
                target.content = root
            }
            is Tab -> {
                target.content = root
            }
            else -> {
                TODO("support other containers")
            }
        }

        return controller
    }
}
