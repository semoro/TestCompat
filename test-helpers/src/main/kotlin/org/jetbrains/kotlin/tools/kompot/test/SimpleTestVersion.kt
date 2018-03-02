package org.jetbrains.kotlin.tools.kompot.test

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionHandler
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionLoader


class SimpleTestVersion(val s: Set<String>) : Version {
    object Default : Version {
        override fun asLiteralValue(): String? {
            return null
        }

        override fun plus(other: Version): Version {
            return other
        }

        override fun contains(other: Version): Boolean {
            return true
        }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            return other == this
        }
    }

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

    override operator fun plus(other: Version): Version {
        if (other == Default) return this
        other as SimpleTestVersion

        return SimpleTestVersion(s + other.s)
    }

    override operator fun contains(other: Version): Boolean {
        if (other == Default) return false
        other as SimpleTestVersion

        return s.containsAll(other.s)
    }
}

class SimpleTestVersionHandler() : VersionHandler {

}

class SimpleTestVersionLoader : VersionLoader {
    override fun load(literalValue: String?): Version {
        literalValue ?: return SimpleTestVersion.Default
        return SimpleTestVersion(literalValue.split(", ").toSet())
    }

}