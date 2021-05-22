plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("nl.jochembroekhoff.gazebo.standalone.cli.Main")
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
