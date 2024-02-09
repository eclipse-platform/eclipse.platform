

FAQ What is LTK?
================

At the EclipseCon conference in 2004, a great deal of interest sparked the idea of adding more generic language IDE infrastructure to Eclipse. Many people have been impressed by the powerful functionality in the Eclipse Java tooling and would like to be able to leverage that support in other languages. This is often currently done by cloning JDT and then hacking out the Java-specific parts and replacing them with a different language. Clearly, this is not very efficient and results in an ongoing effort to catch up to JDT as it continues to add new features.

Eclipse 3.0 includes a first attempt at bubbling up some of the JDT functionality into a generic layer. This generic programming-language tooling layer is called the Eclipse Language Toolkit, or LTK. To start, this generic layer has infrastructure for language-independent refactorings in two new projects:

*   org.eclipse.ltk.core.refactoring
*   org.eclipse.ltk.ui.refactoring

The Refactoring class represents the entire refactoring lifecycle, including precondition checks, generating the set of changes, and post-condition checks. The Change class itself performs more expensive validation on the input to determine whether the refactoring is appropriate and performs the workspace modifications induced by the refactoring. A Change instance can also encapsulate an undo for another change, allowing the user to back out of a refactoring after it has completed. Look for this to be a growing area of innovation in future releases of Eclipse.

See Also:
---------

*   [FAQ How do I participate in a refactoring?](./FAQ_How_do_I_participate_in_a_refactoring.md "FAQ How do I participate in a refactoring?")

