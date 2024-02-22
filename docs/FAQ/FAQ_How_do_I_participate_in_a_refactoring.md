

FAQ How do I participate in a refactoring?
==========================================

JDT has  API to allow other plug-ins to participate in simple refactorings. For example, if a user renames a method, JDT can fix up method references only in other standard Java files. If references to that method exist in Java-like files, such as JSPs, UML diagrams, or elsewhere, the plug-ins responsible for those files will want to update their references as well.

New extension points are defined by org.eclipse.jdt.ui for participation in renaming, creating, deleting, copying, and moving Java elements. The refactoring participant API is based on the new language-independent refactoring infrastructure in the LTK plug-ins. You can find more details by browsing through the extension point documentation for the new refactoring participant extension points.

See Also:
---------

*   [FAQ What is LTK?](./FAQ_What_is_LTK.md "FAQ What is LTK?")

