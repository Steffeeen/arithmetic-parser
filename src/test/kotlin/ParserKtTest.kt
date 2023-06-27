import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ParserKtTest {
    @Test
    fun `simple parse`() {
        val result = parse(toTokenList("5.0 + 3.0 * 9.0"))
        val expectedAst =
            Sum(
                Number(5.0),
                Product(Number(3.0), Number(9.0), Product.ProductSymbol.STAR),
                Sum.SumSymbol.PLUS
            )
        assertIs<ParseResult.Success>(result)
        assertEquals(expectedAst, result.root)
    }

    @Test
    fun `parenthesis and other signs`() {
        val result = parse(toTokenList("(5.5 - 3.3) / .9"))
        val expectedAst = Product(
            Sum(Number(5.5), Number(3.3), Sum.SumSymbol.MINUS),
            Number(0.9),
            Product.ProductSymbol.SLASH
        )
        assertIs<ParseResult.Success>(result)
        assertEquals(expectedAst, result.root)
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

    @Test
    fun `Missing closing parenthesis`() {
        val input = "5.5 * (0.3"
        val result = parse(toTokenList(input))
        assertIs<ParseError>(result)
        println(createErrorMessage(input, Error.Parse(result)))
    }

    @Test
    fun `Missing operators`() {
        val input = "2.0 3.0"
        val result = parse(toTokenList(input))
        assertIs<ParseError>(result)
        println(createErrorMessage(input, Error.Parse(result)))
    }

    @Test
    fun `Missing operands`() {
        val input = "+"
        val result = parse(toTokenList(input))
        assertIs<ParseError>(result)
        println(createErrorMessage(input, Error.Parse(result)))
    }

    @Test
    fun `Unbalanced parentheses`() {
        val input = "(2.0 + 3.0 * 4.0"
        val result = parse(toTokenList(input))
        assertIs<ParseError>(result)
        println(createErrorMessage(input, Error.Parse(result)))
    }

    @Test
    fun `Multiple syntax errors`() {
        val input = "2.0 + * 3.0"
        val result = parse(toTokenList(input))
        assertIs<ParseError>(result)
        println(createErrorMessage(input, Error.Parse(result)))
    }
}