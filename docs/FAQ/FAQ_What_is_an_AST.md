

FAQ What is an AST?
===================

In traditional compiler design, a lexical scanner first converts an input stream into a list of tokens. The tokens are then parsed according to syntax rules for the language, resulting in an abstract syntax tree or AST. Aho, Sethi, and Ullman (1986) distinguish between an _abstract syntax tree_ and a _parse tree_. The AST is used for semantic analysis such as type resolution, and is traditionally converted to an intermediate representation (IR) for optimization and code generation. The Eclipse Java compiler uses a single AST structure from the initial parse all the way through to code generation. This approach allows for heavy optimization of the compiler, avoiding the garbage collection required when using different structures for different phases of the compilation process.

Because it powerfully captures the semantic structure of a Java program, an AST is a very useful data structure for any tools that want to perform complex queries or manipulation of a program. The AST was initially not exposed as API, but many tools were making use of it anyway to perform such manipulation as code refactoring. However, because the compiler’s AST is used for parsing, type resolution, flow analysis, and code generation, the code is very complex and difficult to expose as API. The Java core team decided not to expose its internal AST but instead to expose a clean, new AST. This AST is available in the org.eclipse.jdt.core.dom package. Although this isn’t the same AST used by the compiler, it nonetheless provides a rich semantic representation of a Java program, right down to every expression and statement in methods and initializers. This AST optionally supports resolution of all type references but does not provide advanced capabilities such as flow analysis and code generation. Clients can build and manipulate ASTs for any Java source code, whether or not it’s in the workspace.

See Also:
---------

*   [FAQ What is the Java model?](./FAQ_What_is_the_Java_model.md "FAQ What is the Java model?")
*   Alfred Aho, Ravi Sathi, and Jeffrey Ullman, [_Compilers, Principles, Techniques, and Tools_](http://www-db.stanford.edu/~ullman/dragon.html) (Addison-Wesley, 1986).

