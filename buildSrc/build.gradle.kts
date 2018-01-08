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


apply {
    plugin("kotlin")
}


repositories {
    mavenCentral()
}

dependencies {
    val compile by configurations
    compile(kotlin("stdlib-jdk8", kotlin_version))
    compile("org.ow2.asm:asm-all:5.2")
    compile(gradleApi())
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

