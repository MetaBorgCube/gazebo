package nl.jochembroekhoff.gazebo.standalone.lib.messages

import org.apache.commons.vfs2.FileObject
import org.metaborg.core.messages.IMessage
import org.metaborg.core.messages.IMessagePrinter
import org.metaborg.core.project.IProject

/**
 * Combine multiple [IMessagePrinter]s into a single aggregated printer,
 * which simply dispatches all operations to all its descendants.
 */
class AggregateMessagePrinter(private val printers: Iterable<IMessagePrinter>) : IMessagePrinter {

    override fun print(message: IMessage?, pardoned: Boolean) {
        printers.forEach {
            it.print(message, pardoned)
        }
    }

    override fun print(resource: FileObject?, message: String?, e: Throwable?, pardoned: Boolean) {
        printers.forEach {
            it.print(resource, message, e, pardoned)
        }
    }

    override fun print(project: IProject?, message: String?, e: Throwable?, pardoned: Boolean) {
        printers.forEach {
            it.print(project, message, e, pardoned)
        }
    }

    override fun printSummary() {
        printers.forEach {
            it.printSummary()
        }
    }
}
