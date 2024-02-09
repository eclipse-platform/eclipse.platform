

FAQ How do I write a message to the workbench status line?
==========================================================

When pressing Ctrl+j in a text editor, the editor enters incremental find mode and prints messages in the status bar in the lower left-hand corner.

This can be done from within any view as follows:

      IActionBars bars = getViewSite().getActionBars();
      bars.getStatusLineManager().setMessage("Hello");

Editors can access the status line via IEditorActionBarContributor, which is given a reference to an IActionBars instance in its init method. The contributor is accessed from an editor by using

      IEditorPart.getEditorSite().getActionBarContributor();

Note that the status line is shared by all views and editors. When the active part changes, the status line updates to show the new active part's message.

Parts can also specify an error message on the status line, using the method setErrorMessage. The error message, if provided, always takes precedence over any non&#150;error message that was previously shown. When the error message is cleared, the non&#150;error message is put back on the status line.

