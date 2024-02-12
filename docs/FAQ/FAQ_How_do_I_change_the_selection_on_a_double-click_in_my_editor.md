

FAQ How do I change the selection on a double-click in my editor?
=================================================================

By default, double-clicking in a text editor will cause the complete word under the mouse to be selected. When creating your own text-based editor, you can change this behavior from your SourceConfiguration by overriding the method getDoubleClickStrategy. The method must return an instance of ITextDoubleClickStrategy, a simple interface that gets called whenever the user double-clicks within the editor area.

  
When double-clicking in a text-based editor, the selection will typically change to incorporate the nearest enclosing syntactic unit. For example, clicking next to a brace in the Java editor will expand the selection to include everything in the matched set of braces. Double-clicking in the sample HTML editor will cause the word under the mouse to be selected or, if no word is under the mouse, the entire HTML element. This involves scanning the document forwards and backwards from the current cursor position and then setting the selection accordingly. The following example is a bit contrived and is English-specific, but it illustrates the usual steps by selecting the text range up to the next vowel. If no vowel is found, nothing is selected:

      public void doubleClicked(ITextViewer part) {
         final int offset = part.getSelectedRange().x;
         int length = 0;
         IDocument doc = part.getDocument();
         while (true) {
            char c = doc.getChar(offset + length);
            if (c=='a'||c=='e'||c=='i'||c=='o'||c=='u')
               break;
            if (offset + ++length >= doc.getLength())
               return;
         }
         part.setSelectedRange(offset, length);
      }

  
Of course, double-clicking doesn't have to change the selection. You can perform any manipulation you want on the editor or its document from within a double-click strategy implementation. For example, double-clicking could trigger Content Assist or present possible refactorings. The only real restriction is that you can't use double-click to perform a manipulation on an existing text selection as the first click of the double-click will have eliminated any previous selection.

