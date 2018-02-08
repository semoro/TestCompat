

dependencies {
    val compile by configurations

    compile(project(":common-internals"))
    compile(project(":source-api"))
    compile(project(":tool-api"))
}