package org.jetbrains.kotlin.tools.kompot.test.configuration


import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.task
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet


val SourceSet.kotlin: SourceDirectorySet
    get() =
        (this as HasConvention)
            .convention
            .getPlugin(KotlinSourceSet::class.java)
            .kotlin


fun SourceSet.kotlin(action: SourceDirectorySet.() -> Unit) =
    kotlin.action()

class SSB(val sourceSet: SourceSet, val name: String) {
    fun withJava() {
        sourceSet.java {
            srcDir("src/$name/java")
        }
    }

    fun withKotlin() {
    }
}

fun Project.configureTestSet(p: String, l: SSB.() -> Unit) {

    the<JavaPluginConvention>().sourceSets {
        val sourceSet = maybeCreate(p).also {
            SSB(it, p).l()
        }
        sourceSet.compileClasspath += configurations["compile"]

        val jarTask = task("jar${p.capitalize()}", Jar::class) {
            version = ""
            baseName = p
            from(sourceSet.output)
        }
        tasks.getByName("jar").dependsOn(jarTask)
    }
}