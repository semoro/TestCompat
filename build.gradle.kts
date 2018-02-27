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


val kotlin_version: String by extra

subprojects {

    group = "org.jetbrains.kompot"
    version = "0.0.1"

    apply {
        plugin("kotlin")
    }


    repositories {
        mavenLocal()
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
