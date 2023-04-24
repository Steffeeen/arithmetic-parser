import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LexerKtTest {

    @Test
    fun `simple lex`() {
        val result = lex("+ - * / () 1.23e5")
        val expectedTokens = listOf(
            Token(TPlus, 0),
            Token(TMinus, 2),
            Token(TStar, 4),
            Token(TSlash, 6),
            Token(TLParen, 8),
            Token(TRParen, 9),
            Token(TNumber(123000.0), 11),
            Token(EOF, 17)
        )
        assertIs<LexSuccess>(result)
        assertEquals(expectedTokens, result.tokens)
    }

    @Test
    fun `float literal starting with dot`() {
        val result = lex(".5f")
        assertIs<LexSuccess>(result)
        assertEquals(listOf(Token(TNumber(0.5), 0), Token(EOF, 3)), result.tokens)
    }

    @Test
    fun `multiple floats`() {
        val result = lex(".5 0.3")
        assertIs<LexSuccess>(result)
        assertEquals(listOf(Token(TNumber(0.5), 0), Token(TNumber(0.3), 3), Token(EOF, 6)), result.tokens)
    }

    @Test
    fun `invalid float`() {
        val input = "0.5.f"
        val result = lex(input)
        assertIs<LexError>(result)
    }
}