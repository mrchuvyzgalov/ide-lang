package compiler.lexer.extracter

import compiler.Location
import compiler.lexer.IdentifierToken
import compiler.lexer.Token
import java.lang.StringBuilder

object IdentifierExtractor : TokenExtractor {

    override fun isToken(input: String, currentIndex: Int): Boolean =
        currentIndex < input.length && input[currentIndex].isLetter()

    override fun buildToken(input: String, currentIndex: Int, loc: Location): Token {
        var index = currentIndex
        val sb = StringBuilder()

        while (index < input.length && isAcceptableSymbol(input[index])) {
            sb.append(input[index++])
        }

        return IdentifierToken(sb.toString(), loc)
    }

    private fun isAcceptableSymbol(char: Char) = char.isLetter() || char.isDigit()
}