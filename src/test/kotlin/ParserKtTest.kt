import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ParserKtTest {
    @Test
    fun `simple parse`() {
        val result = parse(toTokenList("5.0 + 3.0 * 9.0"))
        val expectedExpression = Expression(
            Sum(
                product(5.0),
                SumPart.NonEmpty(
                    SumPart.SumSymbol.PLUS,
                    product(3.0, 9.0),
                    SumPart.Empty
                )
            )
        )
        assertIs<ParseResult.Success>(result)
        assertEquals(expectedExpression, result.expression)
    }

    @Test
    fun `parenthesis and other signs`() {
        val result = parse(toTokenList("(5.5 - 3.3) / .9"))
        val innerExpression = Expression(
            Sum(
                product(5.5), SumPart.NonEmpty(
                    SumPart.SumSymbol.MINUS, product(3.3), SumPart.Empty
                )
            )
        )
        val expectedExpression = Expression(
            Sum(
                Product(
                    Primitive.Parenthesis(innerExpression),
                    ProductPart.NonEmpty(
                        ProductPart.ProductSymbol.SLASH,
                        Primitive.Number(0.9),
                        ProductPart.Empty,
                    )
                ),
                SumPart.Empty
            )
        )
        assertIs<ParseResult.Success>(result)
        assertEquals(expectedExpression, result.expression)
    }

    @Test
    fun `error 1`() {
        val result = parse(toTokenList("5.5 * ( .3"))
        assertIs<ParseResult.Error>(result)
    }

    @Test
    fun `error 2`() {
        val result = parse(toTokenList("1.1 2.0"))
        assertIs<ParseResult.Error>(result)
    }

    private fun product(a: Double, b: Double): Product = Product(
        Primitive.Number(a),
        ProductPart.NonEmpty(ProductPart.ProductSymbol.STAR, Primitive.Number(b), ProductPart.Empty)
    )

    private fun product(a: Double): Product = Product(Primitive.Number(a), ProductPart.Empty)

    private fun toTokenList(input: String): List<Token> = when (val result = lex(input)) {
        is LexResult.Success -> result.tokens
        is LexResult.Error -> fail("lex error: ${result.message}")
    }
}