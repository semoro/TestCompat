package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.commons.ClassSignatureNode
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureWriter

class SignatureLoaderImpl : SignatureLoader {
    override val signatureLoadCache = mutableMapOf<String?, ClassSignatureNode>()
}


fun ClassSignatureNode.toSignature(): String? {
    val writer = SignatureWriter()
    this.accept(writer)
    return writer.toString()
}

interface SignatureLoader {

    val signatureLoadCache: MutableMap<String?, ClassSignatureNode>

    fun SSGSignature.loadSignature(): ClassSignatureNode {
        if (signature.isNullOrBlank()) return ClassSignatureNode()
        val reader = SignatureReader(signature)
        val node = ClassSignatureNode()
        reader.accept(node)
        return node
    }

    val SSGSignature.loadedSignature get() = signatureLoadCache.getOrPut(signature, { loadSignature() })
}