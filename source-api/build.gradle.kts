plugins {
    id("maven-publish")
}

dependencies {
    val compileOnly by configurations

    compileOnly(project("intrinsics-stub"))
}

publishing {
    publications {
        val maven by creating(MavenPublication::class) {
            from(components["java"])
        }
    }
}