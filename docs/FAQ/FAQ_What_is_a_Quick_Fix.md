

FAQ What is a Quick Fix?
========================

Whenever it detects an error, the Java editor highlights the error using a wavy red line under the offending code and a marker in the left editor margin. Moving the mouse over the underlined code or the marker will indicate the error. The marker can be selected with the left mouse to activate the Quick Fix pop-up, indicating actions that can be undertaken to repair the error. Alternatively, pressing Ctrl+1 will activate Quick Fix from the keyboard.

Quick Fixes can be used to make typing much faster. Let's assume that you are using Eclipse to rewrite Deep Blue and are writing variation 128839. At some point during editing, you need access to the Rook class. You have not yet written a declaration for Rook, so when you use it in your code, a problem marker will indicate the nonexistence of the type. The marker is normally a simple red cross to indicate an error. If a Quick Fix is available, a small light bulb is shown on top of the error marker.

The JDT can detect syntax errors but is also smart enough to guess what you could do to correct the problem. As can be seen in Figure 3.2, the JDT can create the class for us with a single mouse click.

![](https://github.com/eclipse-platform/eclipse.platform/tree/master/docs/FAQ/images/120px-Quickfix1.jpg)


A major tenet of Extreme Programming is to write test cases first. In other words, use cases are specified first; then the implementation is provided. Quick Fixes help in this process. For instance, we start using our Rook class as if it has a move method. Of course, the editor will complain and place a marker next to the reference to the nonexistent method. However, the editor also guesses what we want to do with the method (Figure 3.3).

![](https://github.com/eclipse-platform/eclipse.platform/tree/master/docs/FAQ/images/120px-Quickguess1.jpg)


In a similar fashion, Quick Fixes can be used to add fields to classes, add parameters to methods, help with unhandled exceptions, and generate local variable declarations. Quick Fixes are designed to allow you to continue the creative process of designing API while using it, a major component of Extreme Programming.

A full list of available Quick Fixes is given at **Help > Help Contents > Java Development User Guide > Concepts > Quick Fix**. Updating quick fixes as you type can be computationally intensive, so you can turn this off via **Window > Preferences > Java > Editor > Annotations > Analyze annotations while typing**.

See Also:
---------

*   [FAQ How do I implement Quick Fixes for my own language?](./FAQ_How_do_I_implement_Quick_Fixes_for_my_own_language.md "FAQ How do I implement Quick Fixes for my own language?")

