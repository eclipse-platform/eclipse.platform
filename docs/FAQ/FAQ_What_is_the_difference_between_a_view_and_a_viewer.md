

FAQ What is the difference between a view and a viewer?
=======================================================

An unfortunate choice of terminology resulted in one of the basic building blocks of the Eclipse workbench having a remarkably similar name to a central construct in JFace. The apprentice Eclipse programmer often falls into the trap of using the terms _view_ and _viewer_ interchangeably and then becomes horribly confused by conflicting accounts of their usage. In reality, they are completely different and fundamentally unrelated constructs. As outlined earlier, JFace viewers are SWT widget adapters that, among other things, perform transformations between model objects and view objects. Views, on the other hand, are one of the two kinds of visible parts that make up a workbench window.

To confuse matters, a view often contains a viewer. The Navigator view, for example, contains a tree viewer. This is not always true, however. A view may contain no viewers, or it may contains several viewers. Viewers can also appear outside of views; for example, in dialogs or editors. In short, views and viewers have no fixed relationship. To put it mathematically, they are orthogonal concepts that often intersect.

See Also:
---------

*   [FAQ What is a viewer?](./FAQ_What_is_a_viewer.md "FAQ What is a viewer?")
*   [FAQ What is a view?](./FAQ_What_is_a_view.md "FAQ What is a view?")

