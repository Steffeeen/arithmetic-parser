
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EvaluatorKtTest {
    @Test
    fun `simple addition`() {
        val result = evaluate(toAst("5.0 + 3.0 + 10.5"))
        assertEquals(18.5, result)
    }

    @Test
    fun `simple subtraction`() {
        val expr = toAst("10.5 - 2.0 - 3.0")
        val result = evaluate(expr)
        assertEquals(5.5, result)
    }

    @Test
    fun `simple multiplication`() {
        val result = evaluate(toAst("5.0 * 3.0 * 2.0"))
        assertEquals(30.0, result)
    }

    @Test
    fun `simple division`() {
        val expr = toAst("30.0 / 2.0 / 3.0")
        val result = evaluate(expr)
        assertEquals(5.0, result)
    }

    @Test
    fun `order of operations`() {
        val result = evaluate(toAst("5.0 + 3.0 * 2.0"))
        assertEquals(11.0, result)
    }

    @Test
    fun `order of operations with parenthesis`() {
        val result = evaluate(toAst("(5.0 + 3.0) * 2.0"))
        assertEquals(16.0, result)
    }

    @Test
    fun `Division by zero`() {
        val input = "5.0 / 0.0"
        val result = evaluate(toAst(input))
        assertEquals(Double.POSITIVE_INFINITY, result)
    }
}