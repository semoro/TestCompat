import org.jetbrains.intellij.IntelliJPluginExtension

plugins {
    id("org.jetbrains.intellij") version "0.2.18"
}

apply {
    plugin("org.jetbrains.intellij")
}

extensions.configure<IntelliJPluginExtension>("intellij") {
    version = "IC-2017.3"
    pluginName = "Kompot"
}
