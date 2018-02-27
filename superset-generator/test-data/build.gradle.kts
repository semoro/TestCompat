import org.jetbrains.kotlin.tools.kompot.test.configuration.configureTestSet

dependencies {
    val compile by configurations
    compile("org.jetbrains:annotations:13.0")
}


configureTestSet("testSimple1") {
    withJava()
}

configureTestSet("testSimple2") {
    withJava()
}

