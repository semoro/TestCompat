import org.jetbrains.kotlin.tools.kompot.test.configuration.configureTestSet

configureTestSet("testSimple") {
    withJava()
}

dependencies {
    compile(project(":source-api"))
}