import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


buildscript {
    val kotlin_version: String by extra

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

val kotlin_version: String by extra


apply {
    plugin("kotlin")
}


repositories {
    mavenCentral()
}

dependencies {
    val compile by configurations
    compile(gradleApi())
    compile(kotlin("stdlib-jdk8", kotlin_version))
    compileOnly(kotlin("gradle-plugin", kotlin_version))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

