import org.junit.jupiter.api.fail

fun toTokenList(input: String): List<Token> = when (val result = lex(input)) {
    is LexResult.Success -> result.tokens
    is LexResult.Error -> fail("lex error: ${result.message}")
}

fun toExpression(input: String): Expression = when (val result = parse(toTokenList(input))) {
    is ParseResult.Success -> result.expression
    is ParseResult.Error -> fail("parse error: ${result.message}")
}