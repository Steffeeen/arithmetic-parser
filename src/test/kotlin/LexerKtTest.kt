import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LexerKtTest {

    @Test
    fun `simple lex`() {
        val result = lex("+ - * / () 1.23e5")
        val expectedTokens = listOf(
            Token.Plus(0),
            Token.Minus(2),
            Token.Star(4),
            Token.Slash(6),
            Token.LParen(8),
            Token.RParen(9),
            Token.Number(11, 123000.0),
            Token.EOF(17)
        )
        assertIs<LexResult.Success>(result)
        assertEquals(expectedTokens, result.tokens)
    }

    @Test
    fun `float literal starting with dot`() {
        val result = lex(".5f")
        assertIs<LexResult.Success>(result)
        assertEquals(listOf(Token.Number(0, 0.5), Token.EOF(3)), result.tokens)
    }

    @Test
    fun `multiple floats`() {
        val result = lex(".5 0.3")
        assertIs<LexResult.Success>(result)
        assertEquals(listOf(Token.Number(0, 0.5), Token.Number(3, 0.3), Token.EOF(6)), result.tokens)
    }

    @Test
    fun `invalid float`() {
        val input = "0.5.f"
        val result = lex(input)
        assertIs<LexError>(result)
    }
}