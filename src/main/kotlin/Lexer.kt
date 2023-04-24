private sealed interface InternalLexResult
private data class InternalLexedToken(val token: Token, val remainingInput: String) : InternalLexResult
private data class InternalNoLexedToken(val remainingInput: String) : InternalLexResult
private data class InternalLexError(val message: String) : InternalLexResult

data class Token(val type: TokenType, val index: Int)

sealed interface TokenType
object TPlus : TokenType
object TMinus : TokenType
object TStar : TokenType
object TSlash : TokenType
object TLParen : TokenType
object TRParen : TokenType
data class TNumber(val value: Double) : TokenType

object EOF : TokenType

private fun lexSingleToken(input: String, index: Int): InternalLexResult = when (input.firstOrNull()) {
    null -> InternalLexedToken(Token(EOF, index), "")
    ' ' -> InternalNoLexedToken(input.drop(1))
    '+' -> InternalLexedToken(Token(TPlus, index), input.drop(1))
    '-' -> InternalLexedToken(Token(TMinus, index), input.drop(1))
    '*' -> InternalLexedToken(Token(TStar, index), input.drop(1))
    '/' -> InternalLexedToken(Token(TSlash, index), input.drop(1))
    '(' -> InternalLexedToken(Token(TLParen, index), input.drop(1))
    ')' -> InternalLexedToken(Token(TRParen, index), input.drop(1))
    '.', in '0'..'9' -> parseFloatLiteral(input, index)
    else -> InternalLexError("Unexpected char ${input.first()} at index $index")
}

private fun parseFloatLiteral(input: String, index: Int): InternalLexResult {
    data class Transition(val fromState: Int, val chars: List<Char>, val toState: Int)

    fun t(fromState: Int, chars: List<Char>, toState: Int) = Transition(fromState, chars, toState)
    fun t(fromState: Int, char: Char, toState: Int) = Transition(fromState, listOf(char), toState)
    val digits = (0..9).map { it.digitToChar() }
    val transitions = listOf(
        t(0, '.', 1),
        t(0, digits, 2),
        t(1, digits, 3),
        t(2, digits, 2),
        t(2, '.', 3),
        t(3, digits, 3),
        t(3, listOf('e', 'E'), 4),
        t(3, listOf('f', 'F', 'l', 'L'), 7),
        t(4, listOf('-', '+'), 5),
        t(4, digits, 6),
        t(5, digits, 6),
        t(6, digits, 6),
        t(6, listOf('f', 'F', 'l', 'L'), 7),
    )
    val acceptingStates = listOf(3, 6, 7)

    var currentState = 0
    var inputIndex = 0

    fun lexResult(): InternalLexResult = if (currentState in acceptingStates) InternalLexedToken(
        Token(
            TNumber(
                input.take(inputIndex).replace(Regex("[fFlL]"), "").toDouble()
            ), index
        ), input.drop(inputIndex)
    ) else InternalLexError("Failed to parse float at index ${index + inputIndex}")
    while (true) {
        val char = input.getOrNull(inputIndex) ?: return lexResult()
        val transition = transitions.find { it.fromState == currentState && char in it.chars } ?: return lexResult()
        currentState = transition.toState
        inputIndex++
    }
}

interface LexResult
data class LexSuccess(val tokens: List<Token>) : LexResult
data class LexError(val message: String) : LexResult

fun lex(input: String): LexResult {
    val tokens = mutableListOf<Token>()
    var remainingInput = input
    var currentIndex = 0

    fun updateValues(newRemainingInput: String) {
        currentIndex += (remainingInput.length - newRemainingInput.length)
        remainingInput = newRemainingInput
    }

    while (true) {
        when (val result = lexSingleToken(remainingInput, currentIndex)) {
            is InternalLexError -> return LexError(result.message)
            is InternalLexedToken -> {
                tokens += result.token
                updateValues(result.remainingInput)
                if (result.token.type == EOF) {
                    return LexSuccess(tokens)
                }
            }

            is InternalNoLexedToken -> {
                updateValues(result.remainingInput)
            }
        }
    }
}


