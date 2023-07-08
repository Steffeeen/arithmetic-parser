import org.junit.jupiter.api.fail

fun toTokenList(input: String): List<Token> = when (val result = lex(input)) {
    is LexResult.Success -> result.tokens
    is LexError -> fail("lex error: $result")
}

fun toAst(input: String): AstNode = when (val result = parse(toTokenList(input))) {
    is ParseResult.Success -> result.root
    is ParseResult.Failed -> fail("parse error: ${result.errors}")
}