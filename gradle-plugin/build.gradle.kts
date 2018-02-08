
dependencies {
    val compile by configurations

    compile(gradleApi())

    compile(project(":post-processor"))
    compile(project(":superset-generator"))
    compile(project(":verifier"))
}
