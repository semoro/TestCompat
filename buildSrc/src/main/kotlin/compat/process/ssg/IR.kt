package compat.process.ssg

import compat.process.Version

class SSGClass(
        var acc: Int,
        var fqName: String,
        var superType: String?,
        var interfaces: Array<String>?,
        var version: Version?
) : SSGNode<SSGClass> {

    val methods = mutableMapOf<String, SSGMethod>()
    val fields = mutableMapOf<String, SSGField>()


    override fun mergeWith(other: SSGClass) {
        this.version = this.version?.plus(other.version) ?: other.version
        other.methods.values.forEach { appendMethod(it) }
        other.fields.values.forEach { appendField(it) }
    }

    fun appendField(node: SSGField) {
        fields[node.fqd()]?.mergeWith(node) ?: run {
            fields[node.fqd()] = node
        }
    }

    fun appendMethod(node: SSGMethod) {
        methods[node.fqd()]?.mergeWith(node) ?: run {
            methods[node.fqd()] = node
        }
    }

    override fun toString(): String {
        return buildString {
            append(version.forDisplay())
            appendln("${acc.toString(16)} class $fqName {")
            methods.values.joinTo(this, separator = "\n\t", prefix = "\t")
            fields.values.joinTo(this, separator = "\n\t", prefix = "\n\t")
            appendln()
            appendln("}")
        }
    }
}

class SSGField(
        var name: String,
        var desc: String,
        var signature: String?,
        var value: Any?,
        var version: Version?
) : SSGNode<SSGField> {
    override fun mergeWith(other: SSGField) {
        this.version = this.version?.plus(other.version) ?: other.version
    }

    fun fqd(): String = name + desc


    override fun toString(): String {
        return version.forDisplay() + "field $name $desc"
    }
}

private fun Version?.forDisplay(): String {
    if (this == null) return ""
    return "@ExistsIn(${this.asLiteralValue()}) "
}

class SSGMethod(
        var name: String,
        var desc: String,
        var signature: String?,
        var exceptions: Array<String>?,
        var version: Version?
) : SSGNode<SSGMethod> {


    override fun mergeWith(other: SSGMethod) {
        this.version = this.version?.plus(other.version) ?: other.version
    }

    fun fqd(): String {
        return name + desc
    }

    override fun toString(): String {
        return version.forDisplay() + "method $name $desc"
    }
}

interface SSGNode<T : SSGNode<T>> {
    fun mergeWith(other: T)
}