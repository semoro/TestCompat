
plugins {
    id("maven-publish")
}

publishing {
    publications {
        val maven by creating(MavenPublication::class) {
            from(components["java"])
        }
    }
}