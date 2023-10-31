package compiler.lexer.extracter

import compiler.Location
import compiler.lexer.Token

sealed interface TokenExtractor {

    fun isToken(input: String, currentIndex: Int): Boolean
    fun buildToken(input: String, currentIndex: Int, loc: Location): Token
}
