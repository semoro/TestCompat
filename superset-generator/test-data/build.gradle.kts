import org.gradle.api.internal.HasConvention
import org.gradle.jvm.tasks.Jar
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

fun configureTestSet(p: String, l: SSB.() -> Unit) {
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

dependencies {
    val compile by configurations
    compile("org.jetbrains:annotations:13.0")
}


configureTestSet("testSimple1") {
    withJava()
}

configureTestSet("testSimple2") {
    withJava()
}

