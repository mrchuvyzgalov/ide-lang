package compiler.analysis.type

import compiler.ast.*

class TypeAnalyzer(private val tree: AstNode, private val links: Map<AstNode, ADeclaration>) {

    private val types = mutableMapOf<AstNode, AtomicType>()
    private val rules = mutableListOf<TypeRule>()
    private val unifyCollection = UnionFindCollection<IdlType>()

    fun analyze(): Map<AstNode, AtomicType> {
        if (tree is AProgram) {
            tree.commands.forEach { analyze(it) }

            initUnifyCollection()

            while (needToUnify()) {
                unify()
            }

            inferTypes()

            return types
        }
        error("Cannot analyse types with $tree")
    }

    private fun needToUnify(): Boolean {
        unifyCollection.parent.forEach {
            if (it.value !is AtomicType) return true
        }
        return false
    }

    private fun initUnifyCollection() {
        rules.forEach {
            unifyCollection.add(it.left)
            unifyCollection.add(it.right)
        }
    }

    private fun unify() {
        rules.forEach {
            unify(it.left, it.right)
        }
    }

    private fun inferTypes() {
        unifyCollection.parent.forEach {
            if (it.key is TypeWrapper && it.value is AtomicType) {
                val wrapper = it.key as TypeWrapper

                if (wrapper.node is AIdentifier || wrapper.node is AVarDeclaration || wrapper.node is AParamDeclaration){
                    val declaration = links[wrapper.node] ?: error("Declaration for ${wrapper.node} was not found")

                    if (types.containsKey(declaration as AstNode)) {
                        if (types[declaration] != it.value) {
                            error("${wrapper.node} have to have ${types[declaration]} type")
                        }
                        types[wrapper.node] = it.value as AtomicType
                    }
                    else {
                        types[wrapper.node] = it.value as AtomicType
                    }
                }
                else if (wrapper.node is AExpr) {
                    types[wrapper.node] = it.value as AtomicType
                }
            }
        }
    }

    private fun unify(type1: IdlType, type2: IdlType) {
        val typeParent1 = unifyCollection.find(type1)
        val typeParent2 = unifyCollection.find(type2)

        if (typeParent1 != typeParent2) {
            if (typeParent1 !is AtomicType && typeParent2 !is AtomicType) {
                unifyCollection.union(typeParent1, typeParent2)
            }
            else if (typeParent1 !is AtomicType && typeParent2 is AtomicType) {
                unifyCollection.union(typeParent1, typeParent2)
            }
            else if (typeParent1 is AtomicType && typeParent2 !is AtomicType) {
                unifyCollection.union(typeParent2, typeParent1)
            }
            else if (isTheSameType(typeParent1, typeParent2)) {
                unifyCollection.union(typeParent1, typeParent2)
            }
            else {
                error("Unification error for $type1 and $type2")
            }
        }
    }

    private fun isTheSameType(type1: IdlType, type2: IdlType): Boolean {
        if (type1 is IntegerType && type2 is IntegerType) return true
        if (type1 is StringType && type2 is StringType) return true
        if (type1 is BooleanType && type2 is BooleanType) return true
        return false
    }

    private fun analyze(node : AstNode) {
        when (node) {
            is AVarDeclaration -> rules.add(TypeRule(TypeWrapper(node), TypeWrapper(node.value as AstNode)))
            is ANumber -> rules.add(TypeRule(TypeWrapper(node), IntegerType))
            is ABoolean -> rules.add(TypeRule(TypeWrapper(node), BooleanType))
            is AString -> rules.add(TypeRule(TypeWrapper(node), StringType))
            is AAssignStmt -> rules.add(TypeRule(TypeWrapper(node.left), TypeWrapper(node.right as AstNode)))
            is AIfStmt -> rules.add(TypeRule(TypeWrapper(node.guard as AstNode), BooleanType))
            is AWhileStmt -> rules.add(TypeRule(TypeWrapper(node.guard as AstNode), BooleanType))
            is ACallExpr -> {
                val declaration = links[node] ?: error("Declaration for $node was not found")

                if (declaration is AFunDeclaration || declaration is AProcDeclaration) {
                    if (declaration is AFunDeclaration) {
                        if (declaration.params.size != node.args.size) error("Arguments number for $node is not ${declaration.params.size}")

                        for (i in declaration.params.indices) {
                            rules.add(TypeRule(TypeWrapper(declaration.params[i]), TypeWrapper(node.args[i] as AstNode)))
                        }

                        rules.add(TypeRule(TypeWrapper(node), TypeWrapper(declaration.stmts.ret.exp as AstNode)))
                    }
                    else if (declaration is AProcDeclaration) {
                        if (declaration.params.size != node.args.size) error("Arguments number for $node is not ${declaration.params.size}")

                        for (i in declaration.params.indices) {
                            rules.add(TypeRule(TypeWrapper(declaration.params[i]), TypeWrapper(node.args[i] as AstNode)))
                        }
                    }
                }
            }
            is ABinaryOp -> {
                rules.add(TypeRule(TypeWrapper(node.left as AstNode), TypeWrapper(node.right as AstNode)))

                when (node.operator) {
                    is Plus -> {
                        rules.add(TypeRule(TypeWrapper(node), TypeWrapper(node.right as AstNode)))
                        rules.add(TypeRule(TypeWrapper(node), TypeWrapper(node.left as AstNode)))
                    }

                    is Minus, is Times, is Divide -> {
                        rules.add(TypeRule(TypeWrapper(node), TypeWrapper(node.right as AstNode)))
                        rules.add(TypeRule(TypeWrapper(node), TypeWrapper(node.left as AstNode)))

                        rules.add(TypeRule(TypeWrapper(node.left), IntegerType))
                        rules.add(TypeRule(TypeWrapper(node.right), IntegerType))
                        rules.add(TypeRule(TypeWrapper(node), IntegerType))
                    }

                    is LessThan, is LessOrEqualThan, is GreaterThan, is GreaterOrEqualThan -> {
                        rules.add(TypeRule(TypeWrapper(node.left), IntegerType))
                        rules.add(TypeRule(TypeWrapper(node.right), IntegerType))
                        rules.add(TypeRule(TypeWrapper(node), BooleanType))
                    }

                    is And, is Or -> {
                        rules.add(TypeRule(TypeWrapper(node), TypeWrapper(node.right as AstNode)))
                        rules.add(TypeRule(TypeWrapper(node), TypeWrapper(node.left as AstNode)))

                        rules.add(TypeRule(TypeWrapper(node.left), BooleanType))
                        rules.add(TypeRule(TypeWrapper(node.right), BooleanType))
                        rules.add(TypeRule(TypeWrapper(node), BooleanType))
                    }

                    is Equal, is NotEqual -> {
                        rules.add(TypeRule(TypeWrapper(node), BooleanType))
                    }
                }
            }
            is AIdentifier -> {
                val declaration = links[node] ?: error("Declaration for $node was not found")
                rules.add(TypeRule(TypeWrapper(node), TypeWrapper(declaration as AstNode)))
            }
            is AUnaryOp -> {
                if (node.operator is Minus) {
                    rules.add(TypeRule(TypeWrapper(node), TypeWrapper(node.subexp as AstNode)))

                    rules.add(TypeRule(TypeWrapper(node), IntegerType))
                    rules.add(TypeRule(TypeWrapper(node.subexp as AstNode), IntegerType))
                }
                else if (node.operator is Not) {
                    rules.add(TypeRule(TypeWrapper(node), TypeWrapper(node.subexp as AstNode)))

                    rules.add(TypeRule(TypeWrapper(node), BooleanType))
                    rules.add(TypeRule(TypeWrapper(node.subexp as AstNode), BooleanType))
                }
            }
        }
        analyzeChildren(node)
    }

    private fun analyzeChildren(node: AstNode) {
        when(node) {
            is AVarDeclarations -> node.declarations.forEach { analyze(it) }
            is AVarDeclaration -> analyze(node.value as AstNode)
            is AAssignStmt -> analyze(node.right as AstNode)
            is AIfStmt -> {
                analyze(node.ifBranch)
                node.elseBranch?.let { analyze(it) }
            }
            is AWhileStmt -> analyze(node.innerBlock)
            is ABlock -> node.body.forEach { analyze(it as AstNode) }
            is ABinaryOp -> {
                analyze(node.left as AstNode)
                analyze(node.right as AstNode)
            }
            is AUnaryOp -> analyze(node.subexp as AstNode)
            is APrintStmt -> analyze(node.exp as AstNode)
            is AFunDeclaration -> {
                analyze(node.stmts.block)
                analyze(node.stmts.ret.exp as AstNode)
            }
            is AProcDeclaration -> analyze(node.stmts.block)
            is ACallExpr -> node.args.forEach { analyze(it as AstNode) }
        }
    }
}