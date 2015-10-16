# Developer - Code style

## General rules
- Variable names should alway be in *CamelCase* and does **not** start with a capital letter
- Class names should alway be in *CamelCase* and does **always** start with a capital letter
- Avoid using `null`, the Option `type` in Scala can be used instead
- If a method/value is designed to be overridden make it a `def` and override it with a `def`, we encourage you to not use `val`