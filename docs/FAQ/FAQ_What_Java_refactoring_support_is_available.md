FAQ What Java refactoring support is available?
===============================================

In the Java perspective, a menu called **Refactor** is enabled. It contains all possible refactoring operations currently implemented by the JDT. All operations are listed, even though they may not be applicable to the current selection. When a given element is selected in the Java editor, the context menu will show the refactoring operations that are specifically applicable to the selection. Refer to the **Help > Help Contents >** Java Development User Guide > Refactoring support **for a comprehensive** discussion on what refactoring techniques are available, how to preview a refactoring, and how to undo/redo refactorings.

It is important to realize the existence of the underlying mechanisms that allow for refactoring. For instance, when a given method is being renamed, all source files that have a reference to the method will have to be visited and their reference to the renamed method changed. The only way to do these kinds of refactorings in a scalable and manageable way is by using an underlying model of the Java source being refactored. Such a model is commonly referred to as a Document Object Model (DOM).

Frequent refactorings for which you may want to remember the keyboard shortcuts are **rename** with shortcut Alt+Shift+R and **extract method** with shortcut Alt+Shift+M. You can create or change key bindings for all refactorings from the **Workbench > Keys** preference page.
  
See Also:
---------

[FAQ How do I support refactoring for my own language?](./FAQ_How_do_I_support_refactoring_for_my_own_language.md "FAQ How do I support refactoring for my own language?")

  
IBM developerWorks article on [refactoring](http://ibm.com/developerworks)

