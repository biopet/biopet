# Developer - Code style

- Variable names should alway be in *camelcase* and do **not** start with a capital letter
- Class names should alway be in *Camelcase* and do **always** start with a capital letter
- Avoid using null, the Option type in scala can be used instead
- If a method/value is designed to be overridden make it a `def` and override it with a `def`, do not use `val`