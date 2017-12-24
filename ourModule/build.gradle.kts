import compat.process.VerifyCompatibility


val runtimeV1 by configurations.creating {
    extendsFrom(configurations["runtime"])
}
val runtimeV2 by configurations.creating {
    extendsFrom(configurations["runtime"])
}

val compileOnly by configurations
dependencies {
    compileOnly(project(":apiSuperset"))
    runtimeV1(project(":apiV1"))
    runtimeV2(project(":apiV2"))
}


tasks {

    val runV1 by creating(JavaExec::class) {
        classpath = the<JavaPluginConvention>().sourceSets["main"].output
        classpath += runtimeV1
        main = "our.OurCodeKt"
    }

    val runV2 by creating(JavaExec::class) {
        classpath = the<JavaPluginConvention>().sourceSets["main"].output
        classpath += runtimeV2
        main = "our.OurCodeKt"
    }

    val verify by creating(VerifyCompatibility::class) {
        println(compileOnly.files)
        classpath = compileOnly
        checkpath = the<JavaPluginConvention>().sourceSets["main"].output
    }
}