package compiler.ast.builder

import compiler.ast.*
import compiler.lexer.FuncToken
import compiler.lexer.IdentifierToken
import compiler.lexer.IfToken
import compiler.lexer.ReturnToken
import compiler.syntax.CstNode

object FuncDeclarationBuilder : AstNodeBuilder {

    private const val CHILDREN_NUMBER = 3

    override fun check(node: CstNode): Boolean =
        node.token is FuncToken && node.children.size == CHILDREN_NUMBER

    override fun build(node: CstNode): AstNode {
        val funcName = getFuncName(node.children[0])
        val params = getParams(node.children[1])
        val block = getBlock(node.children[2])
        val funcToken = getFunc(node)

        return AFunDeclaration(funcName, params, block, funcToken.loc)
    }

    private fun getFuncName(node: CstNode): String = getIdentifier(node).value

    private fun getFunc(node: CstNode): FuncToken {
        if (node.token is FuncToken) {
            return node.token
        }
        error("Expected FuncToken, but found ${node.token}")
    }

    private fun getIdentifier(node: CstNode): IdentifierToken {
        if (node.token is IdentifierToken) {
            return node.token
        }
        error("Expected IdentifierToken, but found ${node.token}")
    }

    private fun getParams(node: CstNode): List<AParamDeclaration> =
        node.children.map {
            val id = getIdentifier(it)
            AParamDeclaration(id.value, id.loc)
        }.toList()

    private fun getBlock(node: CstNode): AFunBlockStmt {
        val blockNode = getBlockNode(node.children[0])
        val returnNode = getReturnNode(node)

        val block = BlockBuilder.build(blockNode)
        val returnToken = getReturn(returnNode)
        val returnStmt = AReturnStmt(ExpressionBuilder.build(returnNode.children[0]) as AExpr, returnToken.loc)

        return AFunBlockStmt(block, returnStmt, null)
    }

    private fun getReturn(node: CstNode): ReturnToken {
        if (node.token is ReturnToken) {
            return node.token
        }
        error("Expected ReturnToken, but found ${node.token}")
    }

    private fun getReturnNode(node: CstNode): CstNode {
        val commandsNumber = node.children.size - 1
        return node.children[commandsNumber]
    }

    private fun getBlockNode(node: CstNode): CstNode {
        val commandsNumber = node.children.size

        val commands = ArrayDeque<CstNode>()

        commands.addAll(node.children.subList(0, commandsNumber))

        return CstNode(children = commands)
    }
}