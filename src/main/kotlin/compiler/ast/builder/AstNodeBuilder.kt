package compiler.ast.builder

import compiler.ast.AstNode
import compiler.syntax.CstNode

interface AstNodeBuilder {

    fun check(node: CstNode): Boolean

    fun build(node: CstNode): AstNode
}