/*
Expression  -> Sum
Sum         -> Product SumPart
SumPart     -> + Product SumPart | - Product SumPart | e
Product     -> Primitive ProductPart
ProductPart -> * Primitive ProductPart | / Primitive ProductPart | e
Primitive   -> (Expression) | number
*/

sealed interface AstNode

data class Expression(val sum: Sum) : AstNode
data class Sum(val product: Product, val sumPart: SumPart) : AstNode
data class Product(val primitive: Primitive, val productPart: ProductPart) : AstNode

sealed interface SumPart : AstNode {
    enum class SumSymbol { PLUS, MINUS }
    object Empty : SumPart
    data class NonEmpty(val symbol: SumSymbol, val product: Product, val sumPart: SumPart) : SumPart
}


sealed interface ProductPart : AstNode {
    enum class ProductSymbol { STAR, SLASH }
    object Empty : ProductPart
    data class NonEmpty(val symbol: ProductSymbol, val primitive: Primitive, val productPart: ProductPart) :
        ProductPart
}

sealed interface Primitive : AstNode {
    data class Number(val value: Double) : Primitive
    data class Parenthesis(val expression: Expression) : Primitive
}

sealed interface ParseResult {
    data class Success(val expression: Expression) : ParseResult
//    data class Error(val message: String) : ParseResult
}

fun parse(tokens: List<Token>): ParseResult {
    val (expression, remainingTokens) = parseExpression(tokens).onError { return it.toParseError() }
    expect<Token.EOF>(remainingTokens).onError { return it.toParseError() }
    return ParseResult.Success(expression)
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

private fun parseExpression(tokens: List<Token>): ParseSubResult<Expression> {
    val (sum, remainingTokens) = parseSum(tokens).onError { return it }
    return ParseSubResult.Success(Expression(sum), remainingTokens)
}

private fun parseSum(tokens: List<Token>): ParseSubResult<Sum> {
    val (product, remainingTokens) = parseProduct(tokens).onError { return it }
    val (sumPart, remainingTokens2) = parseSumPart(remainingTokens).onError { return it }
    return ParseSubResult.Success(Sum(product, sumPart), remainingTokens2)
}

private fun parseProduct(tokens: List<Token>): ParseSubResult<Product> {
    val (primitive, remainingTokens) = parsePrimitive(tokens).onError { return it }
    val (productPart, remainingTokens2) = parseProductPart(remainingTokens).onError { return it }
    return ParseSubResult.Success(Product(primitive, productPart), remainingTokens2)
}

private fun parseSumPart(tokens: List<Token>): ParseSubResult<SumPart> = when (tokens.first()) {
    is Token.Plus -> {
        val (_, remainingTokens) = expect<Token.Plus>(tokens).onError { return it }
        val (product, remainingTokens2) = parseProduct(remainingTokens).onError { return it }
        val (sumPart, remainingTokens3) = parseSumPart(remainingTokens2).onError { return it }
        ParseSubResult.Success(SumPart.NonEmpty(SumPart.SumSymbol.PLUS, product, sumPart), remainingTokens3)
    }

    is Token.Minus -> {
        val (_, remainingTokens) = expect<Token.Minus>(tokens).onError { return it }
        val (product, remainingTokens2) = parseProduct(remainingTokens).onError { return it }
        val (sumPart, remainingTokens3) = parseSumPart(remainingTokens2).onError { return it }
        ParseSubResult.Success(SumPart.NonEmpty(SumPart.SumSymbol.MINUS, product, sumPart), remainingTokens3)
    }

    else -> ParseSubResult.Success(SumPart.Empty, tokens)
}

private fun parseProductPart(tokens: List<Token>): ParseSubResult<ProductPart> = when (tokens.first()) {
    is Token.Star -> {
        val (_, remainingTokens) = expect<Token.Star>(tokens).onError { return it }
        val (primitive, remainingTokens2) = parsePrimitive(remainingTokens).onError { return it }
        val (productPart, remainingTokens3) = parseProductPart(remainingTokens2).onError { return it }
        ParseSubResult.Success(
            ProductPart.NonEmpty(ProductPart.ProductSymbol.STAR, primitive, productPart),
            remainingTokens3
        )
    }

    is Token.Slash -> {
        val (_, remainingTokens) = expect<Token.Slash>(tokens).onError { return it }
        val (primitive, remainingTokens2) = parsePrimitive(remainingTokens).onError { return it }
        val (productPart, remainingTokens3) = parseProductPart(remainingTokens2).onError { return it }
        ParseSubResult.Success(
            ProductPart.NonEmpty(ProductPart.ProductSymbol.SLASH, primitive, productPart),
            remainingTokens3
        )
    }

    else -> ParseSubResult.Success(ProductPart.Empty, tokens)
}

private fun parsePrimitive(tokens: List<Token>): ParseSubResult<Primitive> = when (tokens.first()) {
    is Token.LParen -> {
        val (_, remainingTokens) = expect<Token.LParen>(tokens).onError { return it }
        val (expression, remainingTokens2) = parseExpression(remainingTokens).onError { return it }
        val (_, remainingTokens3) = expect<Token.RParen>(remainingTokens2).onError { return it }
        ParseSubResult.Success(Primitive.Parenthesis(expression), remainingTokens3)
    }

    is Token.Number -> {
        val (number, remainingTokens) = expect<Token.Number>(tokens).onError { return it }
        ParseSubResult.Success(Primitive.Number(number.value), remainingTokens)
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
