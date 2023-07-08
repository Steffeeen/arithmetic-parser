sealed interface Error {
    data class Parse(val errors: List<ParseError>) : Error
    data class Lex(val lexError: LexError) : Error
}

fun createErrorMessages(input: String, error: Error): String = when (error) {
    is Error.Lex -> createLexErrorMessage(input, error)
    is Error.Parse -> error.errors.joinToString("\n") { createParseErrorMessage(input, it) }
}

private fun createLexErrorMessage(input: String, error: Error.Lex): String = when (val lexError = error.lexError) {
    is LexError.InvalidFloat -> createHighlightedErrorMessage("Invalid float", input, lexError.index)
    is LexError.UnexpectedChar -> createHighlightedErrorMessage(
        "Unexpected char ${lexError.char}",
        input,
        lexError.index
    )
}

private fun createParseErrorMessage(input: String, error: ParseError): String =
    when (error) {
        is ParseError.ExpectedToken -> createHighlightedErrorMessage(
            "Expected token of type ${error.tokenType}",
            input,
            error.index
        )

        is ParseError.ExpectedOneOfTokens -> createHighlightedErrorMessage(
            "Expected a token that has a type of one of the following: ${
                error.expectedTokens.joinToString { it.toString() }
            }", input, error.index
        )
    }

private fun createHighlightedErrorMessage(message: String, input: String, index: Int): String =
    listOf("ERROR", "$message at index $index: ", input, " ".repeat(index) + "^").joinToString("\n")

