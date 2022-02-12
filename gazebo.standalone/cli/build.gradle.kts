plugins {
    kotlin("jvm")
    application
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

    implementation("info.picocli", "picocli", "4.6.1")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "Gazebo-Standalone-CLI",
            "Implementation-Version" to archiveVersion,
        )
    }
}
