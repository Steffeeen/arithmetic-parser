
import org.junit.jupiter.api.Test
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
        val input = "5.5 * ( .3"
        val result = parse(toTokenList(input))
        assertIs<ParseError>(result)
        println(createErrorMessage(input, Error.Parse(result)))
    }

    @Test
    fun `error 2`() {
        val input = "1.1 2.0"
        val result = parse(toTokenList(input))
        assertIs<ParseError>(result)
        println(createErrorMessage(input, Error.Parse(result)))
    }

    private fun product(a: Double, b: Double): Product = Product(
        Primitive.Number(a),
        ProductPart.NonEmpty(ProductPart.ProductSymbol.STAR, Primitive.Number(b), ProductPart.Empty)
    )

    private fun product(a: Double): Product = Product(Primitive.Number(a), ProductPart.Empty)
}