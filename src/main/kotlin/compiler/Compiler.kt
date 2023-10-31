package compiler

import compiler.analysis.type.*
import compiler.ast.*
import compiler.lexer.LexicalAnalyzer
import compiler.link.LinkAnalyzer
import compiler.syntax.SyntaxAnalyzer

class Compiler(text: String) {

    private val ast: AstNode
    private val links: Map<AstNode, ADeclaration>
    private val types: Map<AstNode, IdlType>

    private val integers = mutableMapOf<ADeclaration, Int>()
    private val strings = mutableMapOf<ADeclaration, String>()
    private val booleans = mutableMapOf<ADeclaration, Boolean>()

    init {
        val tokens = LexicalAnalyzer(text).tokenize()
        val cstNodes = SyntaxAnalyzer(tokens).parse()

        ast = AstBuilder(cstNodes).build()
        links = LinkAnalyzer(ast).analyze()
        types = TypeAnalyzer(ast, links).analyze()

//        for (type in types) {
//            if (type.key is AVarDeclaration) {
//                val declaration = type.key as AVarDeclaration
//                println("${declaration.name}: ${type.value}")
//            }
//            else {
//                println("${type.key}: ${type.value}")
//            }
//        }
    }

    fun run() {
        integers.clear()
        strings.clear()
        booleans.clear()

        if (ast is AProgram) {
            ast.commands.forEach {
                when (it) {
                    is APrintStmt -> runPrint(it)
                    is AVarDeclarations -> varDeclarations(it)
                    is AAssignStmt -> assignment(it)
                    is AIfStmt -> ifStmt(it)
                    is AWhileStmt -> whileStmt(it)
                    is ACallExpr -> callStmt(it)
                }
            }
            return
        }
        error("Cannot run program with $ast")
    }

    private fun callStmt(astNode: ACallExpr) {
        val declaration = links[astNode] ?: error("Declaration for $astNode was not found")
        if (declaration is AProcDeclaration) procStmt(astNode)
        else if (declaration is AFunDeclaration) funcStmt(astNode)
    }

    private fun procStmt(astNode: ACallExpr) {
        val declaration = links[astNode] ?:  error("Declaration for $astNode was not found")
        if (declaration is AProcDeclaration) {
            assignParams(astNode, declaration.params)
            declaration.stmts.block.body.forEach { blockCommand(it as AstNode) }
        }
    }

    private fun funcStmt(astNode: ACallExpr): String {
        val declaration = links[astNode] ?:  error("Declaration for $astNode was not found")
        if (declaration is AFunDeclaration) {
            assignParams(astNode, declaration.params)
            declaration.stmts.block.body.forEach { blockCommand(it as AstNode) }

            return expressionResult(declaration.stmts.ret.exp)
        }
        error("Declaration $declaration is not AFunDeclaration")
    }

    private fun funcBoolStmt(astNode: ACallExpr): Boolean {
        val declaration = links[astNode] ?:  error("Declaration for $astNode was not found")
        if (declaration is AFunDeclaration) {
            assignParams(astNode, declaration.params)
            declaration.stmts.block.body.forEach { blockCommand(it as AstNode) }

            return getBoolResult(declaration.stmts.ret.exp)
        }
        error("Declaration $declaration is not AFunDeclaration")
    }

    private fun assignParams(astNode: ACallExpr, params: List<AParamDeclaration>) {
        for (i in astNode.args.indices) {
            if (types[astNode.args[i] as AstNode] is BooleanType) booleans[params[i]] = getBoolResult(astNode.args[i])
            else if (types[astNode.args[i] as AstNode] is StringType) strings[params[i]] = getStringResult(astNode.args[i])
            else if (types[astNode.args[i] as AstNode] is IntegerType) integers[params[i]] = getIntResult(astNode.args[i])
        }
    }

    private fun funcIntStmt(astNode: ACallExpr): Int {
        val declaration = links[astNode] ?:  error("Declaration for $astNode was not found")
        if (declaration is AFunDeclaration) {
            assignParams(astNode, declaration.params)
            declaration.stmts.block.body.forEach { blockCommand(it as AstNode) }

            return getIntResult(declaration.stmts.ret.exp)
        }
        error("Declaration $declaration is not AFunDeclaration")
    }

    private fun funcStringStmt(astNode: ACallExpr): String {
        val declaration = links[astNode] ?:  error("Declaration for $astNode was not found")
        if (declaration is AFunDeclaration) {
            assignParams(astNode, declaration.params)
            declaration.stmts.block.body.forEach { blockCommand(it as AstNode) }

            return getStringResult(declaration.stmts.ret.exp)
        }
        error("Declaration $declaration is not AFunDeclaration")
    }

    private fun whileStmt(astNode: AWhileStmt) {
        while (getBoolResult(astNode.guard)) {
            astNode.innerBlock.body.forEach { blockCommand(it as AstNode) }
        }
    }

    private fun ifStmt(astNode: AIfStmt) {
        if (getBoolResult(astNode.guard)) astNode.ifBranch.body.forEach { blockCommand(it as AstNode) }
        else astNode.elseBranch?.body?.forEach { blockCommand(it as AstNode) }
    }

    private fun blockCommand(astNode: AstNode) {
        when (astNode) {
            is APrintStmt -> runPrint(astNode)
            is AVarDeclarations -> varDeclarations(astNode)
            is AAssignStmt -> assignment(astNode)
            is AIfStmt -> ifStmt(astNode)
            is AWhileStmt -> whileStmt(astNode)
            is ACallExpr -> callStmt(astNode)
        }
    }

    private fun assignment(astNode: AAssignStmt) {
        val declaration = links[astNode.left] ?: error("Declaration for ${astNode.left} was not found")

        if (booleans.containsKey(declaration)) booleans[declaration] = getBoolResult(astNode.right)
        else if (strings.containsKey(declaration)) strings[declaration] = getStringResult(astNode.right)
        else if (integers.containsKey(declaration)) integers[declaration] = getIntResult(astNode.right)
    }

    private fun varDeclarations(astNode: AVarDeclarations) {
        for (node in astNode.declarations) {
            if (types[node] is BooleanType) booleans[node] = getBoolResult(node.value)
            else if (types[node] is StringType) strings[node] = getStringResult(node.value)
            else if (types[node] is IntegerType) integers[node] = getIntResult(node.value)
        }
    }

    private fun runPrint(astNode: APrintStmt) {
        println(expressionResult(astNode.exp))
    }

    private fun expressionResult(node: AExpr): String {
        if (node is ACallExpr) return funcStmt(node)
        else if (types[node as AstNode] is BooleanType) return getBoolResult(node).toString()
        else if (types[node] is StringType) return getStringResult(node)
        else if (types[node] is IntegerType) return getIntResult(node).toString()

        error("Cannot get expression result from $node")
    }

    private fun getStringResult(node: AExpr): String {
        if (node is AString) return node.value
        if (node is ABinaryOp) {
            if (node.operator is Plus) return getStringResult(node.left) + getStringResult(node.right)
        }
        if (node is AIdentifier) {
            val declaration = links[node] ?: error("Declaration for $node was not found")
            return strings[declaration] ?: error("Value for $declaration was not found")
        }
        if (node is ACallExpr) return funcStringStmt(node)
        error("Cannot get string result from $node")
    }

    private fun getIntResult(node: AExpr): Int {
        if (node is ANumber) return node.value
        if (node is AUnaryOp) {
            if (node.operator is Minus) return -getIntResult(node.subexp)
        }
        if (node is ABinaryOp) {
            if (node.operator is Plus) return getIntResult(node.left) + getIntResult(node.right)
            if (node.operator is Minus) return getIntResult(node.left) - getIntResult(node.right)
            if (node.operator is Times) return getIntResult(node.left) * getIntResult(node.right)
            if (node.operator is Divide) return getIntResult(node.left) / getIntResult(node.right)
        }
        if (node is AIdentifier) {
            val declaration = links[node] ?: error("Declaration for $node was not found")
            return integers[declaration] ?: error("Value for $declaration was not found")
        }
        if (node is ACallExpr) return funcIntStmt(node)
        error("Cannot get int result from $node")
    }

    private fun getBoolResult(node: AExpr): Boolean {
        if (node is ABoolean) return node.value
        if (node is AUnaryOp) {
            if (node.operator is Not) return !getBoolResult(node.subexp)
        }
        if (node is ABinaryOp) {
            if (node.operator is And) return getBoolResult(node.left) && getBoolResult(node.right)
            if (node.operator is Or) return getBoolResult(node.left) || getBoolResult(node.right)
            if (node.operator is LessThan) return getIntResult(node.left) < getIntResult(node.right)
            if (node.operator is LessOrEqualThan) return getIntResult(node.left) <= getIntResult(node.right)
            if (node.operator is GreaterThan) return getIntResult(node.left) > getIntResult(node.right)
            if (node.operator is GreaterOrEqualThan) return getIntResult(node.left) >= getIntResult(node.right)
        }
        if (node is AIdentifier) {
            val declaration = links[node] ?: error("Declaration for $node was not found")
            return booleans[declaration] ?: error("Value for $declaration was not found")
        }
        if (node is ACallExpr) return funcBoolStmt(node)
        error("Cannot get boolean result from $node")
    }
}