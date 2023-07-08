A parser and evaluator for arithmetic expressions.

Parses the following grammar:

```
E -> T E'
E' -> + T E' | - T E' | ε
T -> F T'
T' -> * F T' | / F T' | ε
F -> (E) | number
```