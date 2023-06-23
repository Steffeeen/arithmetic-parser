/*
Sum         -> Product SumPart
SumPart     -> + Product SumPart | - Product SumPart | e
Product     -> Primitive ProductPart
ProductPart -> * Primitive ProductPart | / Primitive ProductPart | e
Primitive   -> (Expression) | number
*/

sealed interface AstNode

data class Sum(val left: AstNode, val right: AstNode, val symbol: SumSymbol) : AstNode {
    enum class SumSymbol { PLUS, MINUS }
}

data class Product(val left: AstNode, val right: AstNode, val symbol: ProductSymbol) : AstNode {
    enum class ProductSymbol { STAR, SLASH }
}

data class Number(val value: Double) : AstNode

sealed interface ParseResult {
    data class Success(val root: AstNode) : ParseResult
//    data class Error(val message: String) : ParseResult
}

fun parse(tokens: List<Token>): ParseResult {
    val (sum, remainingTokens) = parseSum(tokens).onError { return it.toParseError() }
    expect<Token.EOF>(remainingTokens).onError { return it.toParseError() }
    return ParseResult.Success(sum)
}

private sealed interface ParserInternalErrors : ParseSubResult<Nothing>, ExpectResult<Nothing>

sealed interface ParseError : ParseResult {
    data class ExpectedToken(val index: Int, val simpleName: String) : ParseError, ParserInternalErrors
    data class ExpectedOneOfTokens(val expectedTokens: List<Token>) : ParseError, ParserInternalErrors
}

private fun ParserInternalErrors.toParseError(): ParseError = this as ParseError

private sealed interface ParseSubResult<out T : AstNode> {
    data class Success<out T : AstNode>(val astNode: T, val remainingTokens: List<Token>) : ParseSubResult<T>
}

private inline fun <T : AstNode> ParseSubResult<T>.onError(handler: (parseError: ParserInternalErrors) -> Nothing): ParseSubResult.Success<T> =
    when (this) {
        is ParserInternalErrors -> handler(this)
        is ParseSubResult.Success<T> -> this
    }

private fun parseSum(tokens: List<Token>): ParseSubResult<AstNode> {
    val (product, remainingTokens) = parseProduct(tokens).onError { return it }
    return parseSumPart(remainingTokens, product).onError { return it }
}

private fun parseProduct(tokens: List<Token>): ParseSubResult<AstNode> {
    val (primitive, remainingTokens) = parsePrimitive(tokens).onError { return it }
    return parseProductPart(remainingTokens, primitive).onError { return it }
}

private fun parseSumPart(tokens: List<Token>, left: AstNode): ParseSubResult<AstNode> = when (tokens.first()) {
    is Token.Plus -> {
        val (_, remainingTokens) = expect<Token.Plus>(tokens).onError { return it }
        val (right, remainingTokens2) = parseProduct(remainingTokens).onError { return it }
        val sum = Sum(left, right, Sum.SumSymbol.PLUS)
        parseSumPart(remainingTokens2, sum).onError { return it }
    }

    is Token.Minus -> {
        val (_, remainingTokens) = expect<Token.Minus>(tokens).onError { return it }
        val (right, remainingTokens2) = parseProduct(remainingTokens).onError { return it }
        val sum = Sum(left, right, Sum.SumSymbol.MINUS)
        parseSumPart(remainingTokens2, sum).onError { return it }
    }

    is Token.EOF, is Token.RParen -> ParseSubResult.Success(left, tokens)

    else -> ParseError.ExpectedOneOfTokens(
        listOf(
            Token.Plus(tokens.first().index),
            Token.Minus(tokens.first().index),
            Token.RParen(tokens.first().index),
            Token.EOF(tokens.first().index)
        )
    )
}

private fun parseProductPart(tokens: List<Token>, left: AstNode): ParseSubResult<AstNode> = when (tokens.first()) {
    is Token.Star -> {
        val (_, remainingTokens) = expect<Token.Star>(tokens).onError { return it }
        val (primitive, remainingTokens2) = parsePrimitive(remainingTokens).onError { return it }
        val product = Product(left, primitive, Product.ProductSymbol.STAR)
        parseProductPart(remainingTokens2, product).onError { return it }
    }

    is Token.Slash -> {
        val (_, remainingTokens) = expect<Token.Slash>(tokens).onError { return it }
        val (primitive, remainingTokens2) = parsePrimitive(remainingTokens).onError { return it }
        val product = Product(left, primitive, Product.ProductSymbol.SLASH)
        parseProductPart(remainingTokens2, product).onError { return it }
    }

    is Token.Plus, is Token.Minus, is Token.RParen, is Token.EOF -> ParseSubResult.Success(left, tokens)

    else -> ParseError.ExpectedOneOfTokens(
        listOf(
            Token.Star(tokens.first().index),
            Token.Slash(tokens.first().index),
            Token.Plus(tokens.first().index),
            Token.Minus(tokens.first().index),
            Token.RParen(tokens.first().index),
            Token.EOF(tokens.first().index),
        )
    )
}

private fun parsePrimitive(tokens: List<Token>): ParseSubResult<AstNode> = when (tokens.first()) {
    is Token.LParen -> {
        val (_, remainingTokens) = expect<Token.LParen>(tokens).onError { return it }
        val (sum, remainingTokens2) = parseSum(remainingTokens).onError { return it }
        val (_, remainingTokens3) = expect<Token.RParen>(remainingTokens2).onError { return it }
        ParseSubResult.Success(sum, remainingTokens3)
    }

    is Token.Number -> {
        val (number, remainingTokens) = expect<Token.Number>(tokens).onError { return it }
        ParseSubResult.Success(Number(number.value), remainingTokens)
    }

    else -> ParseError.ExpectedOneOfTokens(
        listOf(
            Token.LParen(tokens.first().index),
            Token.Number(tokens.first().index, 0.0)
        )
    )
}

private sealed interface ExpectResult<out T : Token> {
    data class Success<T : Token>(val token: T, val remainingTokens: List<Token>) : ExpectResult<T>
}

private inline fun <T : Token> ExpectResult<T>.onError(handler: (parseError: ParserInternalErrors) -> Nothing): ExpectResult.Success<T> =
    when (this) {
        is ParserInternalErrors -> handler(this)
        is ExpectResult.Success<T> -> this
    }

private inline fun <reified T : Token> expect(tokens: List<Token>): ExpectResult<T> {
    val first = tokens.first()
    if (first !is T) {
        return ParseError.ExpectedToken(first.index, T::class.simpleName!!)
    }
    return ExpectResult.Success(first, tokens.drop(1))
}
