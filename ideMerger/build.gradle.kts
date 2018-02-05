import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.2.10"

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", kotlin_version))
    }

}

plugins {
    `kotlin-dsl`
}

var kotlin_version: String by extra
kotlin_version = "1.2.10"

logging.captureStandardError(LogLevel.INFO)
logging.captureStandardOutput(LogLevel.INFO)

apply {
    plugin("kotlin")
}


repositories {
    mavenCentral()

    maven {
        setUrl("https://dl.bintray.com/jetbrains/intellij-plugin-service")
    }
}

dependencies {
    val compile by configurations
    compile(gradleApi())
    compile("org.jetbrains.intellij.plugins:structure-ide-classes:3.19")
    compile("org.jetbrains.intellij.plugins:structure-intellij-classes:3.19")
    compile(project(":buildSrc"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

