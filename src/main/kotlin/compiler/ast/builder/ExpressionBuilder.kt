package compiler.ast.builder

import compiler.ast.*
import compiler.lexer.*
import compiler.syntax.CstNode

object ExpressionBuilder : AstNodeBuilder {
    override fun check(node: CstNode): Boolean = error("Not supported")

    override fun build(node: CstNode): AstNode {
        if (node.token is NumberToken) {
            return ANumber(node.token.value.toInt(), node.token.loc)
        }
        if (node.token is StringToken) {
            return AString(node.token.value, node.token.loc)
        }
        if (node.token is FalseToken || node.token is TrueToken) {
            return ABoolean(node.token is TrueToken, node.token.loc)
        }
        if (node.token is IdentifierToken) {
            return getIdOrCall(node)
        }

        return getOperation(node)
    }

    private fun getIdOrCall(node: CstNode): AstNode {
        if (node.children.size == 0) {
            val id = getIdentifier(node)
            return AIdentifier(id.value, id.loc)
        }
        else if (node.children.size == 1) {
            return getCall(getIdentifier(node), node.children[0]) as AstNode
        }
        error("Expected id or call expression, but found $node")
    }

    private fun getCall(id: IdentifierToken, params: CstNode): AExpr {
        val callParams = params.children.map { build(it) as AExpr }.toList()
        return ACallExpr(id.value, callParams, id.loc)
    }

    private fun getIdentifier(node: CstNode): IdentifierToken {
        if (node.token is IdentifierToken) {
            return node.token
        }
        error("Expected IdentifierToken, but found ${node.token}")
    }

    private fun getOperation(node: CstNode): AstNode {
        if (node.children.size == 1) return getUnaryOperation(node)
        if (node.children.size == 2) return getBinaryOperation(node)

        error("Expected operator in $node")
    }

    private fun getUnaryOperation(node: CstNode): AUnaryOp {
        val subexpr = build(node.children[0]) as AExpr
        val operator = getUnaryOperator(node.token ?: error("Expected operator in expression"))

        return AUnaryOp(operator, subexpr, node.token.loc)
    }

    private fun getBinaryOperation(node: CstNode): ABinaryOp {
        val leftExpr = build(node.children[0]) as AExpr
        val rightExpr = build(node.children[1]) as AExpr
        val operator = getBinaryOperator(node.token ?: error("Expected operator in expression"))

        return ABinaryOp(operator, leftExpr, rightExpr, node.token.loc)
    }

    private fun getUnaryOperator(token: Token): UnaryOperator {
        if (token is MinusToken) return Minus
        if (token is NotToken) return Not

        error("Expected unary operator, but was found $token")
    }

    private fun getBinaryOperator(token: Token): BinaryOperator {
        if (token is PlusToken) return Plus
        if (token is MinusToken) return Minus
        if (token is TimesToken) return Times
        if (token is DivideToken) return Divide
        if (token is AndToken) return And
        if (token is OrToken) return Or
        if (token is NotEqualToken) return NotEqual
        if (token is EqualToken) return Equal
        if (token is GreaterOrEqualThanToken) return GreaterOrEqualThan
        if (token is GreaterThanToken) return GreaterThan
        if (token is LessOrEqualThanToken) return LessOrEqualThan
        if (token is LessThanToken) return LessThan

        error("Expected binary operator, but was found $token")
    }
}