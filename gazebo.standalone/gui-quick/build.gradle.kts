plugins {
    kotlin("jvm")
    application
    id("org.openjfx.javafxplugin") version "0.0.10"
}

application {
    mainClass.set("nl.jochembroekhoff.gazebo.standalone.gui_quick.Main")
    applicationDefaultJvmArgs = listOf(
        "-Xss16M",
        "-Dorg.slf4j.simpleLogger.log.org.metaborg=debug",
        "-Dorg.slf4j.simpleLogger.log.nl.jochembroekhoff=debug"
    )
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":langsapi"))
    implementation(project(":defaultlibs"))

    implementation("org.fxmisc.richtext:richtextfx:0.10.9")
}

javafx {
    version = "11.0.2"
    modules = listOf("javafx.controls", "javafx.fxml")
}
