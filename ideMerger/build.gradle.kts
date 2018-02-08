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
    compile(project(":superset-generator"))
}


