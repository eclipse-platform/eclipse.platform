

FAQ How do I add hover support to my text editor?
=================================================

Contents
--------

*   [1 If you're using the Generic and Extensible editor](#If-yoUre-using-the-Generic-and-Extensible-editor)
*   [2 If you're extensing the StructuredTextEditor](#If-youre-extensing-the-StructuredTextEditor)
*   [3 If it's your own editor](#If-its-your-own-editor)
*   [4 Example of ITextHover implementation](#Example-of-ITextHover-implementation)
*   [5 See Also](#See-Also)

If you're using the Generic and Extensible editor
-------------------------------------------------

Add hover support via the org.eclipse.ui.genericeditor.hoverProviders extension point, providing an implemention ITextHover.

If you're extensing the StructuredTextEditor
--------------------------------------------

TODO

If it's your own editor
-----------------------

In [FAQ How do I write an editor for my own language?](./FAQ_How_do_I_write_an_editor_for_my_own_language.md "FAQ How do I write an editor for my own language?") we describe how text hover is enabled for our editor through our configuration class:

 

       class Configuration extends SourceViewerConfiguration {
          ...
          public ITextHover getTextHover(ISourceViewer sv, 
           String contentType) {
             return '''new EScriptTextHover()''';
          }
          ...
       }

Example of ITextHover implementation
------------------------------------

When the user moves the mouse over an area that corresponds to a given node in our AST, it is easy for us to provide a symbolic description of the node. Namely, the editor framework helps out by registering for the mouse events, setting timers, calling us at the right time, and drawing the box that will show the text hover. All that we need to do is match a certain location in the editor to a symbolic string.We do this by providing our own implementation of org.eclipse.jface.text.ITextHover as follows:

 

       public class EScriptTextHover implements ITextHover {
          public IRegion getHoverRegion(ITextViewer tv, int off) {
             return new Region(off, 0);
          }
          public String getHoverInfo(ITextViewer tv, IRegion r) {
             try {
                IDocument doc = tv.getDocument();
                EscriptModel em = EscriptModel.getModel(doc, null);
                return em.getElementAt(r.getOffset()).
                   getHoverHelp();
             }
             catch (Exception e) {            
                return ""; 
             }
          }
       }

The first method we implement is meant for optimizing the drawing of the text hover. We answer the question, If I am going to show a hover for character x in the text viewer, for what region should the hover be the same? We don't try to be too smart here. We simply return an empty region.

The next method implements the real logic of the text hover. We convert the current cursor location to an AST element in the document and ask it to return a string relevant to the current context. Note that we assume that the EscriptModel implements a cache and that the getModel method is inexpensive as we will call it many times during editing.

  

See Also
--------

[FAQ How do I create problem markers for my compiler?](./FAQ_How_do_I_create_problem_markers_for_my_compiler.md "FAQ How do I create problem markers for my compiler?")

