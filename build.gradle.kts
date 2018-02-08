import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.jetbrains.kotlin"
version = "1.0-SNAPSHOT"

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


var kotlin_version: String by extra
kotlin_version = "1.2.10"

subprojects {

    apply {
        plugin("kotlin")
    }


    repositories {
        mavenCentral()
    }

    dependencies {
        val compile by configurations
        compile(kotlin("stdlib-jdk8", kotlin_version))
        compile(kotlin("reflect", kotlin_version))
        compile(kotlin("test-junit", kotlin_version))
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

}
