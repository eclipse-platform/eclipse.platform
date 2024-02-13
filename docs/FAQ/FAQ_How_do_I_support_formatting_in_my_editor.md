

FAQ How do I support formatting in my editor?
=============================================

The JFace source viewer has infrastructure for supporting content formatters. A content formatter's job is primarily to adjust the whitespace between words in a document to match a configured style. A JFace formatter can be configured to operate on an entire document or on a region within a document. Typically, if a document contains several content types, a different formatting strategy will be used for each type. As usual, a formatter is installed from your subclass of SourceViewerConfiguration. To provide a configured formatter instance, override the method getContentFormatter. Most of the time, you can create an instance of the standard formatting class, MultiPassContentFormatter. This class requires that you specify a single _master_ formatting strategy and optionally a _slave_ formatting strategy for each partition in your document.

The following snippet from the Java source configuration installs a master strategy (JavaFormattingStrategy) that is used to format Java code and a slave formatting strategy for formatting comments:

      MultiPassContentFormatter formatter= 
         new MultiPassContentFormatter(
         getConfiguredDocumentPartitioning(viewer), 
         IDocument.DEFAULT_CONTENT_TYPE);
      formatter.setMasterStrategy(
         new JavaFormattingStrategy());
      formatter.setSlaveStrategy(
         new CommentFormattingStrategy(...), 
         IJavaPartitions.JAVA_DOC);

  
The work of formatting the characters in the document is performed by the formatting-strategy classes that are installed on the formatter. JFace doesn't provide much common infrastructure for doing this formatting as it is based largely on the syntax of the language you are formatting.

  
Finally, you will need to create an action that invokes the formatter. No generic formatting action is defined by the text infrastructure, but it is quite easy to create one of your own. The action's run method can simply call the following on the source viewer to invoke the formatter:

   sourceViewer.doOperation(ISourceViewer.FORMAT);

See Also:
---------

[FAQ How do I get started with creating a custom text editor?](./FAQ_How_do_I_get_started_with_creating_a_custom_text_editor.md "FAQ How do I get started with creating a custom text editor?")

