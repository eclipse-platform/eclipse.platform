

FAQ What kinds of viewers does JFace provide?
=============================================

JFace includes the following basic types of viewers:

*   TreeViewer connects to an SWT Tree widget.
*   TableViewer connects to an SWT Table widget.
*   ListViewer connects to an SWT List widget. Note that because the native List widget cannot display icons, most applications use a TableViewer with a single column instead.

A TableTree is a table whose first column can contain a tree, and each row of the table corresponds to a single item in the tree. The Properties view by default uses a TableTree.

*   TextViewer connects to an SWT StyledText widget.
*   CheckboxTreeViewer and CheckboxTableViewer are just like their namesakes, but have a check box next to each tree and table entry.

In addition to these basic viewer types, you are free to implement your own or to subclass an existing viewer to obtain specialized functionality. Viewers, however, are designed to be extremely customizable without the need to create a subclass.

As a historical footnote, an earlier version of JFace used viewer subclassing as the mechanism for customization. This led to a profusion of "monolithic" subclasses that were specialized for a particular purpose. The problem with subclassing is that it provides only one dimension of reuse. If you wanted to create a new viewer that filtered its contents like viewer _X_ but displayed its contents like viewer _Y_, you were stuck with subclassing the viewer with the most contents in common and using copy and paste or other ad-hoc forms of reuse to get what you needed. With the current JFace viewer architecture, each pluggable component provides a new dimension of reuse. You can still subclass a viewer for very specialized situations, but you can generally get away with instantiating one of the standard viewers and installing the necessary pieces to get the behavior you want.


See Also:
---------

*   [FAQ What is a viewer?](./FAQ_What_is_a_viewer.md "FAQ What is a viewer?")
*   [FAQ Why should I use a viewer?](./FAQ_Why_should_I_use_a_viewer.md "FAQ Why should I use a viewer?")
*   [JFace code snippets](http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.jface.snippets/)

