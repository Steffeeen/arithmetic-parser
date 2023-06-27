import kotlin.math.max
import kotlin.math.min

sealed interface Error {
    data class Parse(val parseError: ParseError) : Error
    data class Lex(val lexError: LexError) : Error
}

fun createErrorMessage(input: String, error: Error): String = when (error) {
    is Error.Lex -> createLexErrorMessage(input, error)
    is Error.Parse -> createParseErrorMessage(input, error)
}

private fun createLexErrorMessage(input: String, error: Error.Lex): String = when (val lexError = error.lexError) {
    is LexError.InvalidFloat -> createHighlightedErrorMessage("Invalid float", input, lexError.index)
    is LexError.UnexpectedChar -> createHighlightedErrorMessage(
        "Unexpected char ${lexError.char}",
        input,
        lexError.index
    )
}

private fun createParseErrorMessage(input: String, error: Error.Parse): String =
    when (val parseError = error.parseError) {
        is ParseError.ExpectedToken -> createHighlightedErrorMessage(
            "Expected token of type ${parseError.simpleName}",
            input,
            parseError.index
        )

        is ParseError.ExpectedOneOfTokens -> createHighlightedErrorMessage(
            "Expected a token that has a type of one of the following : ${
                parseError.expectedTokens.joinToString { it.displayString() }
            }", input, parseError.expectedTokens.first().index
        )
    }

private fun Token.displayString(): String = this::class.simpleName!!

private fun createHighlightedErrorMessage(message: String, input: String, index: Int): String {
    val firstLine = "ERROR"
    val (inputLine, highlightLine) = createShownIndex(input, index)
    val secondLineBase = "$message at $index: "
    val secondLine = secondLineBase + inputLine
    val thirdLine = " ".repeat(secondLineBase.length) + highlightLine
    return listOf(firstLine, secondLine, thirdLine).joinToString("\n")
}

private const val CONTEXT_SIZE = 10
private fun createShownIndex(input: String, index: Int): Pair<String, String> {
    return if (input.length > CONTEXT_SIZE) {
        val firstLine = (input + " ".repeat(CONTEXT_SIZE)).substring(
            max(index - CONTEXT_SIZE, 0),
            min(index + CONTEXT_SIZE, input.length + CONTEXT_SIZE)
        )
        val secondLine = " ".repeat(CONTEXT_SIZE) + "^"
        firstLine to secondLine
    } else {
        val secondLine = " ".repeat(index) + "^"
        input to secondLine
    }
}