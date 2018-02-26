

plugins {
    id("maven-publish")
}


dependencies {
    val compile by configurations

    compile(project(":common-internals"))
    compile(project(":source-api"))
    compile(project(":tool-api"))
}


publishing {
    publications {
        val maven by creating(MavenPublication::class) {
            from(components["java"])
        }
    }
}