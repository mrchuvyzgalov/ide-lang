package compiler.syntax

import compiler.lexer.Token

data class CstNode(val token: Token? = null,
                   val children: ArrayDeque<CstNode> = ArrayDeque())