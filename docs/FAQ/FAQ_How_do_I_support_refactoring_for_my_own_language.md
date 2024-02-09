

FAQ How do I support refactoring for my own language?
=====================================================

Refactoring is the process of restructuring code for the purpose of readability, performance improvements, reuse, or simply for the regular evolutionary quest for elegance. The kind of refactoring your language will support is heavily dependent on the nature of your language. Almost all languages provide support for expressing _abstraction_. Therefore, it makes most sense to focus on the processes of encapsulating certain expressions into a more abstract form, such as _extracting a method_, and the reverse, such as _in-lining a method_.

To implement refactorings, you need the UI mechanisms for implementing them. Implement a new menu with your refactoring options; use the JDT for inspiration. See the org.eclipse.jdt.ui plug-in's manifest file and open the **Refactor** menu. Note how all possible refactorings are exhaustively listed here. Each refactoring can be fired by the user, and Java code will have to determine whether the refactoring is appropriate for the given selection and context. Figure 19.6 shows the refactoring menu for the Java perspective.


  
In addition to having a menu in the global menu bar, you will want to add a pop-up menu to your editor to activate a given refactoring from the current selection. Here you can be more creative and restrict the choices given to the user to relate directly to a given selection. Figure 19.7 shows the context menu for the JDT showing the refactoring options for a few selected statements.

Now that we have decided what UI support to provide, how do we actually implement the refactorings? As we said earlier, refactorings can be expressed as a restructuring of code. Let us think. What do we have that describes the structure of our program? Right, the DOM. In the case of JDT, all refactoring is directly expressed as operations on the Java model. The fact that an editor is open and will redraw the changes as a result is just a side-effect. Many refactorings go beyond the current text file. Imagine renaming a given method name. You will have to visit all the places where the original method is called and rename each instance accordingly. Without a model, this is very difficult to implement.

In Eclipse 3.0, the generic portions of the JDT refactoring infrastructure were pushed down into a generic IDE layer called the Eclipse Language Toolkit (LTK). The LTK projects provide a model for refactoring operations, a mechanism to allow third parties to participate in your refactorings, and some basic UI components for refactoring wizards. This infrastructure is a logical starting point for writing refactoring support for your own language.
  
  
The price of success is adoption. When you release your language plug-ins, you have to assume the worst: that people may like them. You may end up with many programmers who use them. However, these programmers are very much like you and will probably want to enhance your tools.

One of the first things people will want to do is obtain access to your DOM to do code generation, extraction, and analysis. The second thing they will want to do is provide their own refactorings and easily tie them to your existing refactorings. Prepare for this to happen and define your own extension point schema before starting to implement any refactorings. If you define all your refactorings using your own extension point schema, you will iron out the bugs, and people will be grateful once they start using your language IDE.

See Also:
---------

[FAQ How do I create an Outline view for my own language editor?](./FAQ_How_do_I_create_an_Outline_view_for_my_own_language_editor.md "FAQ How do I create an Outline view for my own language editor?")

[FAQ What is LTK?](./FAQ_What_is_LTK.md "FAQ What is LTK?")

