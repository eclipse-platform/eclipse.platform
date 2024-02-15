FAQ How do I write an editor for my own language?
=================================================

Contributing to Generic and Extensible editor
---------------------------------------------

Since Eclipse 4.7.M3, you can consider simply adding extensions to the [Generic Editor](https://www.eclipse.org/eclipse/news/4.7/M3/#generic-editor) . This will allow you to write support for textual edition of a language with less boiler plate.

Implementing your own editor
----------------------------

An editor contributes to the org.eclipse.ui.editors extension point, and, in practice, the class implementing the editor is typically a subclass of org.eclipse.ui.editors.text.TextEditor. The simplest way to familiarize yourself with the Eclipse editor framework is by creating a new plug-in with a sample XML editor (use **New > Plug-in Development > Plug-in Project > ... > ... > Plug-in with an editor**). This will provide you with an editor supporting syntax color highlighting, [Content Assist](./FAQ_How_do_I_add_Content_Assist_to_my_language_editor.md "FAQ How do I add Content Assist to my language editor?"), [hover help](./FAQ_How_do_I_add_hover_support_to_my_text_editor.md "FAQ How do I add hover support to my text editor?"), and more. Also be sure to check out Chapter 11 of this book, which describes an HTML editor framework. Both the XML and HTML editors show how to design and arrange your code in manageable packages.

If you want to see what the minimalist editor looks like, we did the experiment of reducing our eScript editor to a single source file with the bare minimum code required to make the smallest possible Eclipse editor ever (see Figure 19.3). We don't suggest that you organize your code this way, but it will show you the basic information you will have to provide to give your editor have a professional look and feel with syntax highlighting and hover help.


Here is the structure of our minimalist eScript editor:

      public class Editor extends TextEditor {
         ...
         public Editor() {
            super();
            setSourceViewerConfiguration(new Configuration());
         }
         protected void createActions() {
            ...
         }
         ...
      }

In the constructor, we set up a source viewer configuration, handling such issues as Content Assist, hover help, and instructing the editor what to do while the user types text. In the inherited createActions method the editor creates its Content Assist action, used when Ctrl+Space is pressed in the editor.

Our configuration looks like this:

      class Configuration extends SourceViewerConfiguration {
         public IPresentationReconciler getPresentationReconciler(
            ISourceViewer sourceViewer) {
            PresentationReconciler pr = new PresentationReconciler();
            DefaultDamagerRepairer ddr = new DefaultDamagerRepairer('''new Scanner()''');
            pr.setRepairer(ddr, IDocument.DEFAULT_CONTENT_TYPE);
            pr.setDamager(ddr, IDocument.DEFAULT_CONTENT_TYPE);
            return pr;
         }
         IContentAssistant getContentAssistant(ISourceViewer sv) {
            ContentAssistant ca = new ContentAssistant();
            IContentAssistProcessor cap = '''new CompletionProcessor()''';
            ca.setContentAssistProcessor(cap, IDocument.DEFAULT_CONTENT_TYPE);
            ca.setInformationControlCreator(getInformationControlCreator(sv));
            return ca;
         }
         public ITextHover getTextHover(ISourceViewer sv, String contentType) {
            return '''new TextHover()''';
         }
      }

We use the default presentation reconciler, and we do not distinguish between sections in our documents. In other words, reconciliation of layout will be the same all over the document, whether we are inside a feature, a plug-in, or a method. We declare a scanner, implemented by us, and rely on the text editor framework to parse the document using our parser when it suits it.

Next, we enable Content Assist by creating a default Content Assistant and defining our own Content Assist processor. When Content Assist is activated, our processor will map the current cursor position to a node in the abstract syntax tree for the underlying document and present relevant continuations based on the currently entered string.

Finally, we create a text-hover that will return a relevant string to be shown in a hover window when we move over a given node in our abstract syntax tree.

For scanning the underlying document to draw it using different colors and fonts, we deploy RuleBasedScanner, one of the simplest scanners offered by the editor framework:

      class Scanner extends RuleBasedScanner {
         public Scanner() {
            WordRule rule = new WordRule(new IWordDetector() {
               public boolean isWordStart(char c) { 
               return Character.isJavaIdentifierStart(c); 
               }
               public boolean isWordPart(char c) {   
                  return Character.isJavaIdentifierPart(c); 
               }
            });
            Token keyword = new Token(new TextAttribute(Editor.KEYWORD, null, SWT.BOLD));
            Token comment = new Token(new TextAttribute(Editor.COMMENT));
            Token string = new Token(new TextAttribute(Editor.STRING));
            //add tokens for each reserved word
            for (int n = 0; n < Parser.KEYWORDS.length; n++) {
               rule.addWord(Parser.KEYWORDS[n], keyword);
            }
            setRules(new IRule[] {
               rule,
               new SingleLineRule("#", null, comment),
               new SingleLineRule("\\"", "\\"", string, '\\\'),
               new SingleLineRule("'", "'", string, '\\\'),
               new WhitespaceRule(new IWhitespaceDetector() {
                  public boolean isWhitespace(char c) {
                     return Character.isWhitespace(c);
                  }
               }),
            });
         }
      }

For each of the keywords in our little language, we define a word entry in our WordRule. We pass our keyword detector, together with rules for recognizing comments, strings, and white spaces to the scanner. With this simple set of rules, the scanner can segment a stream of bytes into sections and then use the underlying rules to color the sections.

See Also:
---------

*   [FAQ How do I create my own editor?](./FAQ_How_do_I_create_my_own_editor.md "FAQ How do I create my own editor?")
*   [FAQ How do I get started with creating a custom text editor?](./FAQ_How_do_I_get_started_with_creating_a_custom_text_editor.md "FAQ How do I get started with creating a custom text editor?")
*   [FAQ How do I add Content Assist to my language editor?](./FAQ_How_do_I_add_Content_Assist_to_my_language_editor.md "FAQ How do I add Content Assist to my language editor?")

