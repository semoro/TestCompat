

dependencies {
    val compile by configurations

    compile(project(":common-internals"))
    compile(project(":tool-api"))

    testCompile(project(":test-helpers"))
}


val test by tasks

test.dependsOn("test-data:jar")
