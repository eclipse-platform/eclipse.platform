

FAQ What is the difference between highlight range and selection?
=================================================================

ITextEditor has two similar concepts for singling out a portion of the editor contents: _selection_ and _highlight range_. The selection is the highlighted segment of text typically set by the user when dragging the mouse or moving the caret around while holding the shift key. The selection can be obtained programmatically via the editor's selection provider:

      ITextEditor editor = ...;//the text editor instance
      ISelectionProvider sp = editor.getSelectionProvider();
      ISelection selection = sp.getSelection();
      ITextSelection text = (ITextSelection)selection;

The selection can also be changed using the selection provider, but ITextEditor provides a convenience method, selectAndReveal, that will change the selection and also scroll the editor so that the new selection is visible.

Highlight range also defines a subset of the editor contents, but it cannot be directly manipulated by the user. Its most useful feature is that the editor can be toggled to show only the current highlight range. This is used in the Java editor to support the Show source of selected element only mode. The default implementation of ITextEditor also links the highlight range to the ISourceViewer concept of _range indication_. The source viewer in turn creates an annotation in the vertical ruler bar that shades the portion of the editor corresponding to the highlight range. To use the Java editor as an example again, you'll notice this shading indicates the range of the method currently being edited. The following snippet sets the highlight range of a text editor and then instructs the editor to display only the highlighted portion:

      ITextEditor editor = ...;
      editor.setHighlightRange(offset, length, true);
      editor.showHighlightRangeOnly(true);

