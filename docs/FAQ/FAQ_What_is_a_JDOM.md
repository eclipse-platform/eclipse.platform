FAQ What is a JDOM?
===================

![Warning2.png](https://github.com/eclipse-platform/eclipse.platform/tree/master/docs/FAQ/images/Warning2.png)

**JDT's JDOM has been superseded by the [DOM/AST](https://help.eclipse.org/topic/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/AST.html), and should no longer be used.**  

  
A JDOM is a Java document object model. DOM is a commonly used term for an object-oriented representation of the structure of a file. A Google definition search turned up this Web definition: _Document Object Model: DOM is a platform- and language-neutral interface, that provides a standard model of how the objects in an XML object are put together, and a standard interface for accessing and manipulating these objects and their interrelationships._

-[Google search for define:Document Object Model](http://www.google.com/search?q=define:Document+Object+Model)

In the context of JDT, the JDOM represents a hierarchical, in-memory representation of a single Java file (compilation unit). The DOM can be traversed to view the elements that make up that compilation unit, including types, methods, package declarations, import statements, and so on. The main purpose of the JDOM API is manipulating Java code, allowing you to add and delete methods and fields, for example. All this manipulation occurs on the in-memory object model, so it does not affect the Java file on disk until the DOM is saved. However, the DOM allows you to access only the _principal structure_ of the compilation unit, so you cannot easily modify document elements, such as javadoc comments and method bodies.

The class ChangeReturnTypeAction in the FAQ Example plug-in uses the JDOM to change the return type of a selected method. Here is the portion of the action that creates and manipulates the JDOM:

      String oldContents = ...;//original file contents
      IMethod method = ...;//the method to change
      String returnType = ...;//the new return type
      ICompilationUnit cu = method.getCompilationUnit();
      String unitName = cu.getElementName();
      String typeName = method.getParent().getElementName();
      String mName = method.getElementName();
      DOMFactory fac = new DOMFactory();
      IDOMCompilationUnit unit = fac.createCompilationUnit(oldContents, unitName);
      IDOMType type = (IDOMType) unit.getChild(typeName);
      IDOMMethod domMethod = (IDOMMethod) type.getChild(mName);
      domMethod.setReturnType(returnType);

Note that modifications to the DOM occur on a copy of the file contents in memory. If the DOM is modified and then discarded, any changes will also be discarded. The current string representation of the in-memory DOM can be obtained by calling IDOMCompilationUnit.getContents(). To save modifications to disk, you should use a working copy.

See Also:
---------

*   [FAQ How do I implement a DOM for my language?](./FAQ_How_do_I_implement_a_DOM_for_my_language.md "FAQ How do I implement a DOM for my language?")
*   [FAQ How do I manipulate Java code?](./FAQ_How_do_I_manipulate_Java_code.md "FAQ How do I manipulate Java code?")
*   [FAQ What is a working copy?](./FAQ_What_is_a_working_copy.md "FAQ What is a working copy?")

