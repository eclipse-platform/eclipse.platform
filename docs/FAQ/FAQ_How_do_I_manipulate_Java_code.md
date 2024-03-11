

FAQ How do I manipulate Java code?
==================================

JDT offers a number of mechanisms for manipulating Java programs. It can be a daunting task to figure out which of these options best suits your needs. Each mechanism has different capabilities and trade-offs, depending on exactly what you want to do. Here is a quick rundown of what's available.

The first option is to use the _Java model_ API. This API is intended primarily for browsing and manipulating Java projects on a macro scale. For example, if you want to create or browse Java projects, packages, or libraries, the Java model is the way to go. The finest granularity the Java model supports is the _principal structure_ of Java types. You can browse the method and field signatures of a type, but you cannot manipulate the bodies of methods or any source file comments. The Java model is typically not used for modifying individual Java files.

The Java document object model (JDOM), is used for manipulating an individual Java file, also known as a _compilation unit_. JDOM also supports only manipulation of the principal structure of Java files but has more power than the Java model for modifying files. In particular, JDOM lets you modify the source characters for each method and field in a file. For performing manipulation of the principal structure only, such as adding methods and changing parameter or return types, JDOM is the way to go.

Last but not least is the _abstract syntax tree (AST)_ API. By creating an AST on a Java file, you have ultimate control over browsing and modifying a Java program, including modification of method bodies and source comments. For complex analysis or modification of Java files, the AST is the best choice. Support for modifying and writing ASTs was introduced in Eclipse 3.0. Prior to 3.0, you could use the AST for analyzing Java types but had to use the JDOM for source modification.

Of course, you can modify Java files without using any of these facilities. You can obtain a raw character buffer on the file contents and perform arbitrary transformations yourself. Regardless of what mechanism you use to modify Java files, you should always use a _working copy_ to do so.

See Also:
---------

*   [FAQ What is a working copy?](./FAQ_What_is_a_working_copy.md "FAQ What is a working copy?")
*   [FAQ What is a JDOM?](./FAQ_What_is_a_JDOM.md "FAQ What is a JDOM?")
*   [FAQ What is an AST?](./FAQ_What_is_an_AST.md "FAQ What is an AST?")

