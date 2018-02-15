

dependencies {
    val compile by configurations

    compile(project(":common-internals"))
    compile(project(":source-api"))
    compile(project(":tool-api"))
    compile(project(":kotlinx.reflect.lite"))

    testCompile("org.slf4j:slf4j-simple:1.7.25")

    testCompile(project(":test-helpers"))
}


val test by tasks

test.dependsOn("test-data:jar")
