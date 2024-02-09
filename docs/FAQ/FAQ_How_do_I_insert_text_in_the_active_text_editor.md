

FAQ How do I insert text in the active text editor?
===================================================

Text editors have no public API to insert text. Furthermore, they do not expose their StyledText widget used to edit the underlying document. Therefore, inserting text in the currently active editor is not trivial. One could start with the SWT shell, search the widget containment hierarchy, and eventually locate the widget to enter text into. Fortunately, an easier way is available.

Text editors obtain a document model by using a _document provider_. This provider functions as synchronizer for multiple editors, notifying each when the other changes the document. By acting as one of the editors on a document, one can easily insert text into the editor of choice. The next code snippet locates the active editor, gets its document provider, requests the underlying document, and inserts some text into it:

      IWorkbenchPage page = ...;
      IEditorPart part = page.getActiveEditor();
      if (!(part instanceof AbstractTextEditor)
         return;
      ITextEditor editor = (ITextEditor)part;
      IDocumentProvider dp = editor.getDocumentProvider();
      IDocument doc = dp.getDocument(editor.getEditorInput());
      int offset = doc.getLineOffset(doc.getNumberOfLines()-4);
      doc.replace(offset, 0, pasteText+"\n");

The provider will notify all other editors to update their presentation as a result.

See Also:
---------

[FAQ How do I use the text document model?](./FAQ_How_do_I_use_the_text_document_model.md "FAQ How do I use the text document model?")

  

