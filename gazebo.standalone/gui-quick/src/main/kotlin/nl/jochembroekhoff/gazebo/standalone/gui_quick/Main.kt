package nl.jochembroekhoff.gazebo.standalone.gui_quick

import javafx.application.Application

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        Application.launch(MainApplication::class.java, *args)
    }
}
