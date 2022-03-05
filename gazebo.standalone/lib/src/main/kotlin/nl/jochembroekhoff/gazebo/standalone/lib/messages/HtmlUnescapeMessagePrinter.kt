package nl.jochembroekhoff.gazebo.standalone.lib.messages

import org.apache.commons.vfs2.FileObject
import org.metaborg.core.messages.IMessage
import org.metaborg.core.messages.IMessagePrinter
import org.metaborg.core.project.IProject

class HtmlUnescapeMessagePrinter(private val printer: IMessagePrinter) : IMessagePrinter by printer {

    override fun print(message: IMessage, pardoned: Boolean) {
        printer.print(HtmlUnescapedMessage(message), pardoned)
    }

    override fun print(resource: FileObject?, message: String, e: Throwable?, pardoned: Boolean) {
        printer.print(resource, HtmlUnescapedMessage.unescape(message), e, pardoned)
    }

    override fun print(project: IProject, message: String, e: Throwable?, pardoned: Boolean) {
        printer.print(project, HtmlUnescapedMessage.unescape(message), e, pardoned)
    }
}
