package nl.jochembroekhoff.gazebo.standalone.cli

import picocli.CommandLine
import kotlin.system.exitProcess

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        exitProcess(CommandLine(CLIApplication()).execute(*args))
    }
}
