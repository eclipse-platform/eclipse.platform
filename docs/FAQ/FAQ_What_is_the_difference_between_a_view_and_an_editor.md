

FAQ What is the difference between a view and an editor?
========================================================

When they first start to write plug-ins that contribute visual components, people are often confused about whether they should write a view or an editor. Superficially, the two appear to be very similar: Both are parts that make up a workbench page, both can contain arbitrary visual subcomponents, and both have various mechanisms for plugging in actions and menus. Let's start with some common misconceptions about the differences between views and editors.

*   Editors display the contents of a file, and views contain groups of files or things other than files. Wrong. Both editors and views can have arbitrary contents from a file or multiple files or be from something that is not a file at all.
*   Editors display text and views display tables or trees. Wrong again. There are no constraints about what goes into an editor or a view. For example, the plug-in Manifest Editor is form-based, whereas the Console view shows plain text.

What are the real differences between views and editors? Here are the main ones.

*   There is generally only one instance of a given view per workbench page, but there can be several instances of the same type of editor.
*   Editors can appear in only one region of the page, whereas views can be moved to any part of the page and minimized as fast views.
*   Editors can be in a dirty state, meaning that their contents are unsaved and will be lost if the editor is closed without saving **\[1\]**.
*   Views have a local toolbar, whereas editors contribute buttons to the global toolbar.
*   Editors can be associated with a file name or an extension, and this association can be changed by users.

**\[1\] Views can also participate in the open.change.save lifecycle by implementing the ISaveablePart interface.**

See Also:
---------

*   [FAQ What is a viewer?](./FAQ_What_is_a_viewer.md "FAQ What is a viewer?")
*   [FAQ What is a view?](./FAQ_What_is_a_view.md "FAQ What is a view?")

  

