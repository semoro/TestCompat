package org.jetbrains.kotlin.tools.kompot.commons

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.VarInsnNode

fun traceUpToVersionConst(insnNode: AbstractInsnNode): String? = traceUpToVersionConstNode(insnNode)?.cst as? String

tailrec fun traceUpToVersionConstNode(insnNode: AbstractInsnNode): LdcInsnNode? {
    val prevNode = insnNode.previous
    when {
        prevNode is LdcInsnNode -> return prevNode
        prevNode is VarInsnNode && prevNode.opcode == Opcodes.ALOAD -> {
            var node = prevNode.previous
            while (node !is VarInsnNode || node.opcode != Opcodes.ASTORE || node.`var` != prevNode.`var`) {
                node = node.previous ?: return null
            }
            return traceUpToVersionConstNode(node)
        }
        prevNode == null -> return null
        else -> return traceUpToVersionConstNode(prevNode)
    }
}