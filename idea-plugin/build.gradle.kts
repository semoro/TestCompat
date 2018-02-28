import org.jetbrains.intellij.IntelliJPluginExtension

plugins {
    id("org.jetbrains.intellij") version "0.2.18"
    kotlin("jvm")
}

apply {
    plugin("org.jetbrains.intellij")
}

repositories {
    maven(url = "http://plugins.jetbrains.com/maven/")
}

extensions.configure<IntelliJPluginExtension>("intellij") {
    version = "IC-2017.3"
    pluginName = "Kompot"
//    plugins { id("kotlin") }
    setPlugins("kotlin")

}

dependencies {
//    compile("com.jetbrains.plugins:org.jetbrains.kotlin:1.2.30-eap-47-IJ2017.3-1:EAP-1.2@zip")
//    compile("com.jetbrains.plugins:org.jetbrains.kotlin:1.2.21-release-eap-47-IJ2017.3-1")
}