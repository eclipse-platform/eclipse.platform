

FAQ How do I add Content Assist to my editor?
=============================================

As with most editor features, Content Assist is added to a text editor from your editor's SourceViewerConfiguration. In this case, you need to override the getContentAssistant method. Here is the implementation of this method in our HTML editor example:

        public IContentAssistant getContentAssistant(ISourceViewer sv) {
            ContentAssistant ca = new ContentAssistant();
            IContentAssistProcessor pr = new TagCompletionProcessor();
            ca.setContentAssistProcessor(pr, HTML_TAG);
            ca.setContentAssistProcessor(pr, IDocument.DEFAULT_CONTENT_TYPE);
            ca.setInformationControlCreator(getInformationControlCreator(sv));
            return ca;
        }

Although IContentAssistant is the top-level type that provides Content Assist, most of the work is done by an IContentAssistProcessor. Documents are divided into _partitions_ to represent different logical segments of the text, such as comments, keywords, and identifiers. The IContentAssistant's main role is to provide the appropriate processor for each partition of your document. In our HTML editor example, the document is divided into three partitions: comments, tags, and everything else, represented by the default content type.

In the preceding snippet, we have installed a single processor for tags and the default content type and no processor for comments. The final line before the return statement sets the information control creator for the content assistant. The information control creator is a factory for creating those information pop-ups that frequently appear in text editors, such as the Java editor. In the context of Content Assist, the information control creator is used to create the information pop-up that provides more details about a recommended completion.

After configuring the content assistant, the next step is to create one or more IContentAssistProcessors. The main method of interest is computeCompletionProposals, which is called when the user invokes Content Assist at a given position in the editor. The method's job is to figure out what completions, if any, are appropriate for that position. The method returns one or more ICompletionProposal instances, which is typically an instance of the generic class CompletionProposal.

A completion proposal encapsulates all the information that the text framework needs for presenting the completions to the user and for inserting a completion if the user selects one. Most of this information is self-explanatory, but a couple of items in the proposal need a bit more information: the context information and the additional proposal information.

Additional proposal info is displayed in a pop-up window when the user highlights a proposal but has not yet inserted it. As the name implies, the purpose of this information is to help the user decide whether the selected proposal is the desired completion. For our HTML tag processor, the additional information is a string describing the function of that tag. This information will be displayed only if your Content Assistant has installed an information control creator. See the earlier snippet of the getContentAssistant method to see how this is done.

Context information, if applicable, is displayed in a pop-up after the user has inserted a completion. The purpose here is to give the user extra information about what text needs to be entered after the completion has been inserted. This is best explained with an example from the Java editor. After the user has inserted a method using Content Assist in the Java editor, context information is used to provide information about the method parameters. As the user types in the method parameters, the context information shows the data type and parameter name for each parameter.

The final step in implementing Content Assist in your editor is to add an action that will allow the user to invoke Content Assist. The text framework provides an action for this purpose, but it is not installed in the abstract text editor because it isn't applicable to all flavors of text editors. The action is installed by overriding your editor's createActions method. The action class is ContentAssistAction. Here is a snippet from the createActions method in our example HTML editor:

        Action action = new ContentAssistAction(resourceBundle, "ContentAssistProposal.", this); 
        String id = ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS
        action.setActionDefinitionId(id);
        setAction("ContentAssistProposal", action); 
        markAsStateDependentAction("ContentAssistProposal", true);

Line 1 creates the Action instance, supplying a resource bundle where the display strings should be taken from, along with a prefix for that action. The message bundle on disk would look something like this:

        ContentAssistProposal.label=Content assist
        ContentAssistProposal.tooltip=Content assist
        ContentAssistProposal.description=Provides Content Assistance

Line 3 associates a well-known ID with the action that will tell the UI's command framework that this is the action for Content Assist. This allows the user to change the key binding for Content Assist generically and have it apply automatically to all editors that provide Content Assist.

Line 4 registers the action with the editor framework, using a unique ID. This ID can be used to identify the action when constructing menus and is used by the editor action bar contributor to reference actions defined by the editor. The final line in the snippet indicates that the action needs to be updated whenever the editor's state changes.

That's it! The Content Assist framework has a lot of hooks to allow you to customize the behavior and presentation of proposals and context information, but it would take far too much space to describe them here. See the sample HTML editor's implementation of Content Assist for a simple example to get you started. For a real-world example, we recommend browsing through the Java editor's Content Assist implementation. It can be found in the package org.eclipse.jdt.internal.ui.text.java in the org.eclipse.jdt.ui plug-in.

See Also:
---------

*   [FAQ How can Content Assist make me the fastest coder ever?](./FAQ_How_can_Content_Assist_make_me_the_fastest_coder_ever.md "FAQ How can Content Assist make me the fastest coder ever?")
*   [FAQ What is a document partition?](./FAQ_What_is_a_document_partition.md "FAQ What is a document partition?")
*   [FAQ How do I add Content Assist to my language editor?](./FAQ_How_do_I_add_Content_Assist_to_my_language_editor.md "FAQ How do I add Content Assist to my language editor?")

