

FAQ How do I add Content Assist to my language editor?
======================================================

Contents
--------

*   [1 If you're using the Generic and Extensible editor](#If-youre-using-the-Generic-and-Extensible-editor)
*   [2 If you're extensing the StructuredTextEditor](#If-youre-extensing-the-StructuredTextEditor)
*   [3 If it's your own editor](#If-its-your-own-editor)
*   [4 Example of IContentAssistProcessor](#Example-of-IContentAssistProcessor)
*   [5 See Also](#See-Also)

If you're using the Generic and Extensible editor
-------------------------------------------------

Add hover support via the org.eclipse.ui.genericeditor.contentAssistProcessors extension point, providing an implemention IContentAssistProcessor.

If you're extensing the StructuredTextEditor
--------------------------------------------

TODO

If it's your own editor
-----------------------

In [FAQ How do I write an editor for my own language?](./FAQ_How_do_I_write_an_editor_for_my_own_language.md "FAQ How do I write an editor for my own language?") we describe how Content Assist is installed through our configuration class, as follows:

 

       class Configuration extends SourceViewerConfiguration {
          public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
             ContentAssistant ca = new ContentAssistant();
             IContentAssistProcessor cap = new EScriptCompletionProcessor(); // <-- See reference to IContentAssistProcess implementation here
             ca.setContentAssistProcessor(cap, IDocument.DEFAULT_CONTENT_TYPE);
             ca.setInformationControlCreator(getInformationControlCreator(sourceViewer));
             return ca;
       }

Example of IContentAssistProcessor
----------------------------------

A completion processor takes the current insertion point in the editor and figures out a list of continuation proposals for the user to choose from. Our completion processor looks something like this:

 

       class EScriptCompletionProcessor implements IContentAssistProcessor { 
          private final IContextInformation[] NO_CONTEXTS = { };
          private final char[] PROPOSAL_ACTIVATION_CHARS = { 's','f','p','n','m', };
          private ICompletionProposal[] NO_COMPLETIONS = { };
     
          public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
             try {
                IDocument document = viewer.getDocument();
                ArrayList result = new ArrayList();
                String prefix = lastWord(document, offset);
                String indent = lastIndent(document, offset);
                EscriptModel model = EscriptModel.getModel(document, null);
                model.getContentProposals(prefix, indent, offset, result);
                return (ICompletionProposal[]) result.toArray(new ICompletionProposal[result.size()]);
             } catch (Exception e) {
                // ... log the exception ...
                return NO_COMPLETIONS;
             }
          }
          private String lastWord(IDocument doc, int offset) {
             try {
                for (int n = offset-1; n &gt;= 0; n--) {
                  char c = doc.getChar(n);
                  if (!Character.isJavaIdentifierPart(c))
                    return doc.get(n + 1, offset-n-1);
                }
             } catch (BadLocationException e) {
                // ... log the exception ...
             }
             return "";
          }
          private String lastIndent(IDocument doc, int offset) {
             try {
                int start = offset-1; 
                while (start &gt;= 0 &amp;&amp; doc.getChar(start)!= '\n') start--;
                int end = start;
                while (end &lt; offset &amp;&amp; Character.isSpaceChar(doc.getChar(end))) end++;
                return doc.get(start+1, end-start-1);
             } catch (BadLocationException e) {
                e.printStackTrace();
             }
             return "";
          }
          public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) { 
             return NO_CONTEXTS;
          }
          char[] getCompletionProposalAutoActivationCharacters() {
             return PROPOSAL_ACTIVATION_CHARS;
          }
          // ... remaining methods are optional ...
       }

Basically, Content Assist completion has three steps. First, we have to figure out what string has already been started by the user (see lastWord). Second, we have to find appropriate completions. Third, we have to return strings so that when they are inserted, they lay out acceptably (see the use of lastIndent).

See Also
--------

*   [FAQ How do I add Content Assist to my editor?](./FAQ_How_do_I_add_Content_Assist_to_my_editor.md "FAQ How do I add Content Assist to my editor?")
*   [FAQ How do I add hover support to my text editor?](./FAQ_How_do_I_add_hover_support_to_my_text_editor.md "FAQ How do I add hover support to my text editor?")

