package nl.jochembroekhoff.gazebo.standalone.gui_quick

import javafx.application.Platform
import javafx.scene.control.TextArea
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.messages.IMessage
import org.metaborg.core.messages.IMessagePrinter
import org.metaborg.core.project.IProject

class TextAreaMessagePrinter(private val dest: TextArea) : IMessagePrinter {
    override fun print(message: IMessage, pardoned: Boolean) {
        Platform.runLater {
            dest.appendText("${message.type().name}: ${message.message()}\n")
        }
    }

    override fun print(resource: FileObject?, message: String, e: Throwable?, pardoned: Boolean) {
        Platform.runLater {
            dest.appendText("${message}\n")
        }
    }

    override fun print(project: IProject, message: String, e: Throwable?, pardoned: Boolean) {
        Platform.runLater {
            dest.appendText("${message}\n")
        }
    }

    override fun printSummary() {}
}
