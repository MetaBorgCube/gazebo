package nl.jochembroekhoff.gazebo.standalone.lib.runner

import org.metaborg.core.messages.IMessagePrinter
import java.io.File

data class GazeboRunnerConfiguration(
    val root: File,
    val logToStdout: Boolean = false,
    val logToFile: Boolean = false,
    val logAux: IMessagePrinter? = null,
)
