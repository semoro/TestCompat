package compat.rt

interface VersionSwitch {
    fun accepts(v: String): Boolean
}