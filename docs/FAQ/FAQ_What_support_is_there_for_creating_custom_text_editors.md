

FAQ What support is there for creating custom text editors?
===========================================================

Eclipse provides rich support for creating editors that operate on text, such as programming language editors or document editors. The framework has been designed in several layers of increasing coupling to the Eclipse Platform. Some of the lower-level components can easily be used outside Eclipse in stand-alone applications, and other parts can be used only within a running Eclipse Platform. Using this framework, you can quickly create a powerful editor with surprisingly little work.

The text infrastructure is so vast that it can be very difficult to figure out where to begin. Here is a little roadmap to the various plug-ins that provide facilities for text processing:

  

*   org.eclipse.text.

This plug-in is one of very few that have no connection to any other plug-ins. Because it has no dependence on the Eclipse Platform or even on SWT, this plug-in can easily be used in a stand-alone application. This plug-in provides a model for manipulating text and has no visual components, so it can be used by headless programs that process or manipulate text. Think of this plug-in as a rich version of java.lang.StringBuffer but with support for event change notification, partitions, search and replace, and other text-processing facilities.

  

*   org.eclipse.swt.

SWT is covered elsewhere in this book, but in the context of text editing, the class StyledText needs to be mentioned here. StyledText is the SWT user-interface object for displaying and editing text. Everything the user sees is rooted here: colors, fonts, selections, the caret (I-beam cursor), and more. You can add all kinds of listeners to this widget to follow what the user is doing. Some of the fancier features include word wrapping; bi-directional text, used by many non-Latin languages; and printing support.

  

*   org.eclipse.jface.text.

This plug-in is the marriage of the model provided by org.eclipse.text and the view provided by StyledText. True to the philosophy of JFace, the intent here is not to hide the SWT layer but to augment the visual presentation with a rich model and controllers. This plug-in is the heart of the text framework, and the list of features it provides is far too long to enumerate. To name just a few, it supports Content Assist, rule-based text scanners and partitioners, a vertical ruler, incremental reconciling, formatters, and hover displays. Many of these features are explored in more detail by other FAQs in this chapter.

  

*   org.eclipse.ui.workbench.texteditor.

This plug-in couples the text framework to the Eclipse Platform. You can't use the features provided here without being part of a running Eclipse workbench. In particular, this plug-in supports text editors that appear in the workbench editor area and features a large collection of Action subclasses for manipulating the contents of an editor, as well as support for annotations, incremental search, and more. If you're designing a text editor for use within the Eclipse Platform, you'll be subclassing the AbstractTextEditor class found in this plug-in. This abstract editor contains most of the functionality of the default text editor in Eclipse but without making any assumptions about where the content being edited is stored; it does not have to be in the workspace. This plug-in is appropriate for use in an RCP application that requires text editing support.

  

*   org.eclipse.ui.editors.

This plug-in provides the main concrete editor in the base Eclipse Platform: the default text editor. You generally don't need to use this plug-in when writing your own editor, as all the useful functionality has been abstracted out into the plug-ins we've already mentioned. This concrete editor is typically used on an IFileEditorInput on an IFile in the local workspace.

  

See Also:
---------

[FAQ I'm still confused. How do all the editor pieces fit together?](./FAQ_Im_still_confused_How_do_all_the_editor_pieces_fit_together.md "FAQ I'm still confused! How do all the editor pieces fit together?")

[FAQ How do I get started with creating a custom text editor?](./FAQ_How_do_I_get_started_with_creating_a_custom_text_editor.md "FAQ How do I get started with creating a custom text editor?")

[FAQ How do I use the text document model?](./FAQ_How_do_I_use_the_text_document_model.md "FAQ How do I use the text document model?")

