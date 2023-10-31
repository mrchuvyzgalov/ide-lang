package compiler.ast

import compiler.ast.builder.*
import compiler.syntax.CstNode

class AstBuilder(private val tree: CstNode) {

    companion object {
        val astBuilders = buildList {
            add(ProcDeclarationBuilder)
            add(FuncDeclarationBuilder)
            add(VarDeclarationBuilder)
            add(IfBuilder)
            add(WhileBuilder)
            add(PrintBuilder)
            add(AssignmentBuilder)
            add(CallBuilder)
        }
    }

    fun build() : AstNode {
        val nodes = mutableListOf<AstNode>()
        for (node in tree.children) {
            nodes.add(visit(node))
        }
        return AProgram(nodes, null)
    }

    private fun visit(node: CstNode): AstNode {
        for (builder in astBuilders) {
            if (builder.check(node)) {
                return builder.build(node)
            }
        }
        error("AstBuilder didn't found suitable ast node builder for node $node")
    }
}