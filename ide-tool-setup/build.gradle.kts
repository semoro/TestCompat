repositories {
    mavenCentral()

    maven {
        setUrl("https://dl.bintray.com/jetbrains/intellij-plugin-service")
    }
}


dependencies {
    compile("org.jetbrains.intellij.plugins:structure-ide-classes:3.19")
    compile(project(":tool-api"))
}
