fun evaluate(expression: Expression): Double {
    return evalNode(expression)
}

private fun evalNode(node: AstNode): Double = when (node) {
    is Expression -> evalNode(node.sum)
    is Primitive.Number -> node.value
    is Primitive.Parenthesis -> evalNode(node.expression)
    is Product -> evalProduct(node)
    is Sum -> evalSum(node)
    is ProductPart.Empty -> error("empty ProductPart in evalNode")
    is ProductPart.NonEmpty -> error("non-empty ProductPart in evalNode")
    is SumPart.Empty -> error("empty SumPart in evalNode")
    is SumPart.NonEmpty -> error("non-empty SumPart in evalNode")
}

private fun evalSum(sum: Sum): Double {
    val product = evalProduct(sum.product)
    return evalSumPart(product, sum.sumPart)
}

private fun evalSumPart(value: Double, sumPart: SumPart): Double = when (sumPart) {
    is SumPart.Empty -> value
    is SumPart.NonEmpty -> {
        val product = evalNode(sumPart.product)
        val newValue = combineWithSymbol(value, product, sumPart.symbol)
        evalSumPart(newValue, sumPart.sumPart)
    }
}

private fun evalProduct(product: Product): Double {
    val primitive = evalNode(product.primitive)
    return evalProductPart(primitive, product.productPart)
}

private fun evalProductPart(value: Double, productPart: ProductPart): Double = when (productPart) {
    is ProductPart.Empty -> value
    is ProductPart.NonEmpty -> {
        val primitive = evalNode(productPart.primitive)
        val newValue = combineWithSymbol(value, primitive, productPart.symbol)
        evalProductPart(newValue, productPart.productPart)
    }
}

private fun combineWithSymbol(a: Double, b: Double, symbol: ProductPart.ProductSymbol): Double = when (symbol) {
    ProductPart.ProductSymbol.STAR -> a * b
    ProductPart.ProductSymbol.SLASH -> a / b
}

private fun combineWithSymbol(a: Double, b: Double, symbol: SumPart.SumSymbol): Double = when (symbol) {
    SumPart.SumSymbol.PLUS -> a + b
    SumPart.SumSymbol.MINUS -> a - b
}