private sealed interface InternalLexResult
private data class InternalLexedToken(val token: Token, val remainingInput: String) : InternalLexResult
private data class InternalNoLexedToken(val remainingInput: String) : InternalLexResult
private data class InternalLexError(val message: String) : InternalLexResult

sealed interface Token {
    val index: Int

    data class Plus(override val index: Int) : Token
    data class Minus(override val index: Int) : Token
    data class Star(override val index: Int) : Token
    data class Slash(override val index: Int) : Token
    data class LParen(override val index: Int) : Token
    data class RParen(override val index: Int) : Token
    data class Number(override val index: Int, val value: Double) : Token
    data class EOF(override val index: Int) : Token
}

private fun lexSingleToken(input: String, index: Int): InternalLexResult = when (input.firstOrNull()) {
    null -> InternalLexedToken(Token.EOF(index), "")
    ' ' -> InternalNoLexedToken(input.drop(1))
    '+' -> InternalLexedToken(Token.Plus(index), input.drop(1))
    '-' -> InternalLexedToken(Token.Minus(index), input.drop(1))
    '*' -> InternalLexedToken(Token.Star(index), input.drop(1))
    '/' -> InternalLexedToken(Token.Slash(index), input.drop(1))
    '(' -> InternalLexedToken(Token.LParen(index), input.drop(1))
    ')' -> InternalLexedToken(Token.RParen(index), input.drop(1))
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

    fun lexResult(): InternalLexResult = if (currentState in acceptingStates) {
        val token = Token.Number(index, input.take(inputIndex).replace(Regex("[fFlL]"), "").toDouble())
        InternalLexedToken(token, input.drop(inputIndex))
    } else InternalLexError("Failed to parse float at index ${index + inputIndex}")
    while (true) {
        val char = input.getOrNull(inputIndex) ?: return lexResult()
        val transition = transitions.find { it.fromState == currentState && char in it.chars } ?: return lexResult()
        currentState = transition.toState
        inputIndex++
    }
}

sealed interface LexResult {
    data class Success(val tokens: List<Token>) : LexResult
    data class Error(val message: String) : LexResult
}

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
            is InternalLexError -> return LexResult.Error(result.message)
            is InternalLexedToken -> {
                tokens += result.token
                updateValues(result.remainingInput)
                if (result.token is Token.EOF) {
                    return LexResult.Success(tokens)
                }
            }

            is InternalNoLexedToken -> {
                updateValues(result.remainingInput)
            }
        }
    }
}


