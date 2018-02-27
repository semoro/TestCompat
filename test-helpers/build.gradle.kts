

val ideaIC by configurations.creating

repositories {
    maven("https://www.jetbrains.com/intellij-repository/snapshots")
    maven("https://www.jetbrains.com/intellij-repository/releases")
}

dependencies {
    ideaIC("com.jetbrains.intellij.idea:ideaIC:2017.3")
}

fun ideaRT(): FileTree {
    return zipTree(ideaIC.singleFile).matching { include("lib/idea_rt.jar") }
}

dependencies {
    val compile by configurations
    compile(ideaRT())
    compile(project(":common-internals"))
}