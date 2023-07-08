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
    data class Failed(val errors: List<ParseError>) : ParseResult
}

sealed interface ParseError {
    data class ExpectedToken(val index: Int, val tokenType: TokenType) : ParseError
    data class ExpectedOneOfTokens(val index: Int, val expectedTokens: List<TokenType>) : ParseError
}

private object FirstSets {
    val SUM = setOf(TokenType.LPAREN, TokenType.NUMBER)
    val SUM_PART = setOf(TokenType.PLUS, TokenType.MINUS, TokenType.EOF)
    val PRODUCT = setOf(TokenType.LPAREN, TokenType.NUMBER)
    val PRODUCT_PART = setOf(TokenType.STAR, TokenType.SLASH, TokenType.EOF)
    val PRIMITIVE = setOf(TokenType.LPAREN, TokenType.NUMBER)
}

fun parse(tokens: List<Token>): ParseResult {
    return Parser(tokens).parse()
}

private class Parser(val initialTokens: List<Token>) {
    var errorMode = false
    val errors: MutableList<ParseError> = mutableListOf()
    val remainingTokens = initialTokens.toMutableList()

    fun parse(): ParseResult {
        val sum = parseSum(setOf(TokenType.EOF))
        expect<Token.EOF>(setOf(TokenType.EOF))

        return if (errors.isEmpty()) {
            ParseResult.Success(sum!!)
        } else {
            ParseResult.Failed(errors)
        }
    }

    private fun parseSum(anchors: Set<TokenType>): AstNode? {
        val product = parseProduct(anchors + FirstSets.SUM_PART)
        return product?.let { parseSumPart(it, anchors) }
    }

    private fun parseProduct(anchors: Set<TokenType>): AstNode? {
        val primitive = parsePrimitive(anchors + FirstSets.PRODUCT_PART)
        return primitive?.let { parseProductPart(it, anchors) }
    }

    private fun parseSumPart(left: AstNode, anchors: Set<TokenType>): AstNode? =
        when (remainingTokens.firstOrNull()) {
            is Token.Plus -> {
                val plusToken = expect<Token.Plus>(anchors + FirstSets.PRODUCT + FirstSets.SUM_PART)
                val right = plusToken?.let { parseProduct(anchors + FirstSets.SUM_PART) }
                val sum = right?.let { Sum(left, it, Sum.SumSymbol.PLUS) }
                sum?.let { parseSumPart(it, anchors) }
            }

            is Token.Minus -> {
                val minusToken = expect<Token.Minus>(anchors + FirstSets.PRODUCT + FirstSets.SUM_PART)
                val right = minusToken?.let { parseProduct(anchors + FirstSets.SUM_PART) }
                val sum = right?.let { Sum(left, it, Sum.SumSymbol.MINUS) }
                sum?.let { parseSumPart(it, anchors) }
            }

            is Token.EOF, is Token.RParen -> left

            else -> {
                val error = ParseError.ExpectedOneOfTokens(
                    errorIndex(),
                    listOf(TokenType.PLUS, TokenType.MINUS, TokenType.RPAREN, TokenType.EOF)
                )
                handleError(error, anchors + FirstSets.SUM_PART)
                if (remainingTokens.isNotEmpty()) {
                    parseSumPart(left, anchors)
                } else {
                    null
                }
            }
        }

    private fun parseProductPart(left: AstNode, anchors: Set<TokenType>): AstNode? =
        when (remainingTokens.firstOrNull()) {
            is Token.Star -> {
                val starToken = expect<Token.Star>(anchors + FirstSets.PRIMITIVE + FirstSets.PRODUCT_PART)
                val primitive = starToken?.let { parsePrimitive(anchors + FirstSets.PRODUCT_PART) }
                val product = primitive?.let { Product(left, it, Product.ProductSymbol.STAR) }
                product?.let { parseProductPart(it, anchors) }
            }

            is Token.Slash -> {
                val slashToken = expect<Token.Slash>(anchors + FirstSets.PRIMITIVE + FirstSets.PRODUCT_PART)
                val primitive = slashToken?.let { parsePrimitive(anchors + FirstSets.PRODUCT_PART) }
                val product = primitive?.let { Product(left, it, Product.ProductSymbol.SLASH) }
                product?.let { parseProductPart(it, anchors) }
            }

            is Token.Plus, is Token.Minus, is Token.RParen, is Token.EOF -> left

            else -> {
                val error = ParseError.ExpectedOneOfTokens(
                    errorIndex(),
                    listOf(
                        TokenType.STAR,
                        TokenType.SLASH,
                        TokenType.PLUS,
                        TokenType.MINUS,
                        TokenType.RPAREN,
                        TokenType.EOF,
                    )

                )
                handleError(error, anchors + FirstSets.PRODUCT_PART)
                if (remainingTokens.isNotEmpty()) {
                    parseProductPart(left, anchors)
                } else {
                    null
                }
            }
        }

    private fun parsePrimitive(anchors: Set<TokenType>): AstNode? = when (remainingTokens.firstOrNull()) {
        is Token.LParen -> {
            val lParenToken = expect<Token.LParen>(anchors + FirstSets.SUM + TokenType.RPAREN)
            val sum = lParenToken?.let { parseSum(anchors + TokenType.RPAREN) }
            val rParenToken = expect<Token.RParen>(anchors)
            rParenToken?.let { sum }
        }

        is Token.Number -> {
            val numberToken = expect<Token.Number>(anchors)
            numberToken?.let { Number(it.value) }
        }

        else -> {
            val error = ParseError.ExpectedOneOfTokens(
                errorIndex(),
                listOf(
                    TokenType.LPAREN,
                    TokenType.NUMBER
                )
            )
            handleError(error, anchors + FirstSets.PRIMITIVE)
            if (remainingTokens.isNotEmpty()) {
                parsePrimitive(anchors)
            } else {
                null
            }
        }
    }

    private inline fun <reified T : Token> expect(anchors: Set<TokenType>): T? {
        if (remainingTokens.isEmpty()) {
            return null
        }

        if (remainingTokens.first() !is T) {
            val error = ParseError.ExpectedToken(errorIndex(), tokenType<T>())
            handleError(error, anchors + tokenType<T>())

            if (remainingTokens.isEmpty()) {
                return null
            }

            if (remainingTokens.first() !is T) {
                // found token from anchor set, don't consume it
                return null
            }
        }

        val first = remainingTokens.removeFirst()
        require(first is T)

        errorMode = false
        return first
    }

    private fun errorIndex(): Int =
        if (remainingTokens.isEmpty()) initialTokens.last().index else remainingTokens.first().index

    private fun handleError(error: ParseError, skipUntilTokenSet: Set<TokenType>) {
        if (!errorMode) {
            errors += error
            errorMode = true
        }
        remainingTokens.removeFirst()
        remainingTokens.removeAll(remainingTokens.takeWhile { it.type !in skipUntilTokenSet })
    }
}

private inline fun <reified T : Token> tokenType(): TokenType = when {
    Token.Plus(0) is T -> TokenType.PLUS
    Token.Minus(0) is T -> TokenType.MINUS
    Token.Star(0) is T -> TokenType.STAR
    Token.Slash(0) is T -> TokenType.SLASH
    Token.LParen(0) is T -> TokenType.LPAREN
    Token.RParen(0) is T -> TokenType.RPAREN
    Token.Number(0, 0.0) is T -> TokenType.NUMBER
    Token.EOF(0) is T -> TokenType.EOF
    else -> error("should never happen")
}
