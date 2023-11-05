package compiler.function

import compiler.ast.*

class FunctionAnalyzer(private val links: Map<AstNode, ADeclaration>) {

    fun analyze(): Map<AReturnStmt, AFunDeclaration> {
        val resultMap = mutableMapOf<AReturnStmt, AFunDeclaration>()

        links.values
            .filterIsInstance<AFunDeclaration>()
            .forEach { func -> analyzeBlock(func.stmts).forEach { resultMap[it] = func } }

        return resultMap
    }

    private fun analyzeBlock(block: ABlock): List<AReturnStmt> = block.body.flatMap { analyzeBlockCommand(it) }

    private fun analyzeBlockCommand(command: ABlockCommand): List<AReturnStmt> {
        if (command is ABlock) return analyzeBlock(command)
        if (command is AReturnStmt) return listOf(command)
        if (command is AWhileStmt) return analyzeBlock(command.innerBlock)
        if (command is AIfStmt) {
            val ifResults = analyzeBlock(command.ifBranch)
            val elseResults = command.elseBranch?.let { analyzeBlock(it) } ?: emptyList()
            return ifResults + elseResults
        }
        return listOf()
    }

}