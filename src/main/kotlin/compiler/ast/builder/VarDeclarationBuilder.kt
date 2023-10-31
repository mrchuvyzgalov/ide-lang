package compiler.ast.builder

import compiler.ast.AExpr
import compiler.ast.AVarDeclaration
import compiler.ast.AVarDeclarations
import compiler.ast.AstNode
import compiler.lexer.IdentifierToken
import compiler.lexer.VarToken
import compiler.syntax.CstNode

object VarDeclarationBuilder : AstNodeBuilder {

    private const val CHILDREN_NUMBER = 1

    override fun check(node: CstNode): Boolean =
        node.token is VarToken && node.children.size == CHILDREN_NUMBER

    override fun build(node: CstNode): AstNode {
        val vars = node.children[0].children.map { buildVarAssignment(it) }
        val varToken = getVar(node)
        return AVarDeclarations(vars, varToken.loc)
    }

    private fun buildVarAssignment(node: CstNode): AVarDeclaration {
        val id = getIdentifier(node.children[0])
        return AVarDeclaration(id.value, ExpressionBuilder.build(node.children[1]) as AExpr, id.loc)
    }

    private fun getVar(node: CstNode): VarToken {
        if (node.token is VarToken) {
            return node.token
        }
        error("Expected VarToken, but found ${node.token}")
    }

    private fun getIdentifier(node: CstNode): IdentifierToken {
        if (node.token is IdentifierToken) {
            return node.token
        }
        error("Expected IdentifierToken, but found ${node.token}")
    }
}