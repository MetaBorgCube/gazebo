plugins {
    kotlin("jvm")
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

application {
    mainClass.set("nl.jochembroekhoff.gazebo.standalone.cli.Main")
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

    implementation("info.picocli", "picocli", "4.6.3")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "Gazebo-Standalone-CLI",
            "Implementation-Version" to archiveVersion,
        )
    }
}
