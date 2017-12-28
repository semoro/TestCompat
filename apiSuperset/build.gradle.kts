import compat.process.GenSuperset
import compat.process.Version
import compat.process.VersionHandler

plugins {
    `java`
}

val apiV1 by configurations.creating {
    extendsFrom(configurations["compileOnly"])
}

val apiV2 by configurations.creating {
    extendsFrom(configurations["compileOnly"])
}

dependencies {
    val compile by configurations
    compile(project(":compileTimeData"))
    apiV1(project(":apiV1"))
    apiV2(project(":apiV2"))
}

object MyVerHandler: VersionHandler {
    override fun plus(t: Version?, other: Version?): Version? {
        if (t == null) return other
        if (other == null) return t
        return MyVer((t as MyVer).versionStrings + (other as MyVer).versionStrings)
    }

    override fun contains(t: Version?, other: Version?): Boolean {
        if (t == null) return true
        if (other == null) return false
        t as MyVer
        other as MyVer
        return other.versionStrings.all { it in t.versionStrings }
    }
}

class MyVer(val versionStrings: List<String>) : Version {
    override fun asLiteralValue(): String {
        return versionStrings.joinToString()
    }


    override fun hashCode(): Int {
        return versionStrings.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MyVer

        if (versionStrings != other.versionStrings) return false

        return true
    }
}

tasks {
    val genSuperset by creating(GenSuperset::class) {
        input += root(apiV1, MyVer(listOf("1")))
        input += root(apiV2, MyVer(listOf("2")))
        versionHandler = MyVerHandler
        dependsOn(apiV1, apiV2)
    }
}