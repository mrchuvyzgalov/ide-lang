package compiler.ast.builder

import compiler.ast.AExpr
import compiler.ast.APrintStmt
import compiler.ast.AstNode
import compiler.lexer.IdentifierToken
import compiler.lexer.PrintToken
import compiler.lexer.ProcToken
import compiler.syntax.CstNode

object PrintBuilder : AstNodeBuilder {

    private const val CHILDREN_NUMBER = 1

    override fun check(node: CstNode): Boolean =
        node.token is PrintToken && node.children.size == CHILDREN_NUMBER

    override fun build(node: CstNode): AstNode {
        val printToken = getPrint(node)
        return APrintStmt(getParam(node.children[0]) as AExpr, printToken.loc)
    }

    private fun getPrint(node: CstNode): PrintToken {
        if (node.token is PrintToken) {
            return node.token
        }
        error("Expected PrintToken, but found ${node.token}")
    }

    private fun getParam(node: CstNode) = ExpressionBuilder.build(node)
}