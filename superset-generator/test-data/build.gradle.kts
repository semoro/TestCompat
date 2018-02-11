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

interface SSB {
    fun withJava()
    fun withKotlin()
}

fun configureTestSet(p: String, l: SSB.() -> Unit) {
    the<JavaPluginConvention>().sourceSets {
        val sourceSet = maybeCreate(p).apply {
            object : SSB {
                override fun withJava() {
                    java {
                        srcDir("src/$p/java")
                    }
                }

                override fun withKotlin() {
                    kotlin {
                        srcDir("src/$p/kotlin")
                    }
                }
            }.l()
        }

        val jarTask = task("jar${p.capitalize()}", Jar::class) {
            version = ""
            baseName = p
            from(sourceSet.output)
        }
        tasks.getByName("jar").dependsOn(jarTask)
    }


}



configureTestSet("testSimple1") {
    withJava()
}

configureTestSet("testSimple2") {
    withJava()
}

