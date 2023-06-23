fun evaluate(node: AstNode): Double {
    return evalNode(node)
}

private fun evalNode(node: AstNode): Double = when (node) {
    is Product -> evalProduct(node)
    is Sum -> evalSum(node)
    is Number -> node.value
}

private fun evalSum(sum: Sum): Double = when (sum.symbol) {
    Sum.SumSymbol.PLUS -> evalNode(sum.left) + evalNode(sum.right)
    Sum.SumSymbol.MINUS -> evalNode(sum.left) - evalNode(sum.right)
}

private fun evalProduct(product: Product): Double = when (product.symbol) {
    Product.ProductSymbol.STAR -> evalNode(product.left) * evalNode(product.right)
    Product.ProductSymbol.SLASH -> evalNode(product.left) / evalNode(product.right)
}
