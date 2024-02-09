

FAQ I'm still confused! How do all the editor pieces fit together?
==================================================================

Start browsing through the editor plug-ins, and a lot of names come out at you: IDocument, StyledText, ISourceViewer, ITextViewer, Text, ITextEditor, and many more. Furthermore, many of these pieces seem to overlap; they often provide similar functionality, and it's not easy to figure out which piece you should be looking at when implementing a feature.

  
It's not easy for the newcomer to grasp, but these overlapping pieces represent carefully designed layers of abstraction that allow for maximum reuse. Eclipse is designed to be extended by a large number of third parties with all kinds of different requirements. Instead of presenting a monolithic API that attempts to cater to all these needs, the editor framework gives you a loosely coupled toolkit that you can draw from, based on the needs of your particular application. A high-level overview helps when you're starting out.

  
The text-editing framework follows the same architectural principles as the rest of the Eclipse Platform. The four layers are the model (core), the view (SWT), the controller (JFace), and the presentation context (usually the workbench). The model and the view are self-contained pieces that know nothing about each other or the rest of the world. If you have a simple GUI application, you can get away with creating the view and manipulating it directly. Some tools operate directly on the model and don't care about the presentation. Often, the model, view, and controller are all used, but the same triad might appear in different contexts: in a workbench part, in a dialog, and so on. If your application demands it, you can replace any of these layers with a completely different implementation but reuse the rest.

  
Figure 15.1 show how these layers map onto the text-editing framework.

The core is org.eclipse.jface.text.IDocument, with no dependency on any UI pieces.

The view is org.eclipse.swt.custom.StyledText. Don't be fooled by org.eclipse.swt.widgets.Text; this is a (usually) native widget with very basic functionality. It's suitable for simple entry fields in dialogs but does not provide rich editing features. StyledText is the real widget for presenting text editors.

The basic controller layer is provided by org.eclipse.jface.text.ITextViewer. This is extended by org.eclipse.jface.text.ISourceViewer to provide features particular to programming language editors. The context for presenting text editors in a workbench part is provided by org.eclipse.ui. texteditor.ITextEditor. This is the text framework extension to the generic editor interface, org.eclipse.ui.IEditorPart.

  

    <img src=../images/texteditor.png>

    **Figure 15.1**   Model-view-controller collaboration in the Eclipse text-editing framework

  

See Also:
---------

[FAQ\_What\_support\_is\_there\_for\_creating\_custom\_text_editors?](./FAQ_What_support_is_there_for_creating_custom_text_editors.md "FAQ What support is there for creating custom text editors?")

[FAQ\_How\_do\_I\_get\_started\_with\_creating\_a\_custom\_text_editor?](./FAQ_How_do_I_get_started_with_creating_a_custom_text_editor.md "FAQ How do I get started with creating a custom text editor?")

