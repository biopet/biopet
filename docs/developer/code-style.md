# Developer - Code style

## General rules
- Variable names should always be in *camelCase* and does **not** start with a capital letter

```scala
// correct: 
val outputFromProgram: String = "foobar"
 
// incorrect:
val OutputFromProgram: String = "foobar"
```

- Class names should always be in *CamelCase* and **always** start with a capital letter

```scala
// correct:
class ExtractReads {}

// incorrect:
class extractReads {}

```

- Avoid using `null`, the Option `type` in Scala can be used instead

```scala
// correct:
val inputFile: Option[File] = None

// incorrect:
val inputFile: File = null

```
- If a method/value is designed to be overridden make it a `def` and override it with a `def`, we encourage you to not use `val`

