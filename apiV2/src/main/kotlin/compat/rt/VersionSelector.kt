package compat.rt

class VersionSelector : VersionSwitch {
    override fun accepts(v: String): Boolean {
        return v == "2"
    }

}