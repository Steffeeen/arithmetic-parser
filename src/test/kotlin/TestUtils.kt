import org.junit.jupiter.api.fail

fun toTokenList(input: String): List<Token> = when (val result = lex(input)) {
    is LexResult.Success -> result.tokens
    is LexError -> fail("lex error: $result")
}

fun toExpression(input: String): Expression = when (val result = parse(toTokenList(input))) {
    is ParseResult.Success -> result.expression
    is ParseError -> fail("parse error: $result")
}