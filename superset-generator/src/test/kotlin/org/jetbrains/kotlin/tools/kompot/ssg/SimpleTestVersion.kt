package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionHandler


class SimpleTestVersion(val s: Set<String>) : Version {
    override fun asLiteralValue(): String {
        return s.joinToString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SimpleTestVersion

        if (s != other.s) return false

        return true
    }

    override fun hashCode(): Int {
        return s.hashCode()
    }
}

class SimpleTestVersionHandler() : VersionHandler {
    override fun plus(t: Version?, other: Version?): Version? {
        if (t == null) return other
        if (other == null) return t
        t as SimpleTestVersion
        other as SimpleTestVersion

        return SimpleTestVersion(t.s + other.s)
    }

    override fun contains(t: Version?, other: Version?): Boolean {
        if (t == null) return true
        if (other == null) return false
        t as SimpleTestVersion
        other as SimpleTestVersion

        return t.s.containsAll(other.s)
    }
}