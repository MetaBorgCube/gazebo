import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    java
    kotlin("jvm") version "1.6.10" apply false
}

allprojects {
    group = "nl.jochembroekhoff.gazebo.standalone"
    version = "1.0.0"

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://artifacts.metaborg.org/content/repositories/releases/")
        maven("https://artifacts.metaborg.org/content/repositories/snapshots/")
        maven("https://nexus.usethesource.io/content/repositories/releases/")
    }
}

subprojects {
    apply {
        plugin("java")
    }

    dependencies {
        implementation("org.metaborg", "org.metaborg.spoofax.core", "2.5.16")
        implementation("org.slf4j", "slf4j-simple", "1.7.36")
    }

    dependencies {
        testImplementation(kotlin("test-junit5"))
        testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.8.2")
        testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.8.2")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    tasks.test {
        useJUnitPlatform()
    }
}
