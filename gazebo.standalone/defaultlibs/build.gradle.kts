plugins {
    kotlin("jvm")
    id("com.github.harbby.gradle.serviceloader") version("1.1.8")
}

dependencies {
    api(project(":langsapi"))
}

serviceLoader {
    serviceInterfaces.add("nl.jochembroekhoff.gazebo.standalone.langsapi.GzbsLanguagesProvider")
}

tasks.jar {
    properties["gzbsLibsDir"]?.let {
        from(it) {
            include("*.spoofax-language", "gzbs-builtin-langs.txt")
        }
    }
}
