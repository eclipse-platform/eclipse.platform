FAQ How do I get started with creating a custom text editor?
============================================================

Contributing to Generic and Extensible editor
---------------------------------------------

Since Eclipse 4.7.M3, you can consider simply adding extensions to the [Generic Editor](https://www.eclipse.org/eclipse/news/4.7/M3/#generic-editor) . This will allow you to write support for textual edition of a language with less boiler plate.

Create a custom text editor
---------------------------

Follow the steps in[FAQ\_How\_do\_I\_create\_my\_own_editor?](./FAQ_How_do_I_create_my_own_editor.md "FAQ How do I create my own editor?") to create a platform editor extension, but instead of the basic EditorPart, subclass AbstractTextEditor. You don't need to override createPartControl this time, because the abstract editor builds the visual representation for you. In fact, you need to do nothing more; simply subclassing AbstractTextEditor will give you a generic text editor implementation right out of the box.

To customize your editor, you need to create your own subclass of SourceViewerConfiguration defined in package org.eclipse.jface.text.source when the editor is created. This class is the locus of all editor customization. Just about every time you want to add a feature to a text editor, you start by subclassing a method in the configuration. Browse through the methods of this class to get an idea of the kinds of customization you can add.

Another entry point for editor customization is the document provider. The editor's document provider is a factory method for supplying the model object (an IDocument) that represents the editor's contents. The document provider's main function is to transform an IEditorInput into an appropriate IDocument. By subclassing the generic document provider, you can create a customized document, such as a document that is divided into multiple partitions or a document that uses a different character encoding. This is also a good place for adding listeners to the document so you can be notified when it changes.

You'll also want to customize the actions available to your editor. The abstract text editor provides some actions, but if you want to add extra tools, you'll need actions for them. This is done by overriding the method createActions in your editor. Be sure to call super.createActions to allow the abstract editor to install the default set of text-editing actions, such as **Cut**, **Copy**, **Paste**, and **Undo**. The editor framework supplies more actions that are not automatically added to the abstract editor, but you can add them from your implementation of createActions. Look in the package org.eclipse.ui.texteditor for more available actions.

The FAQs in this chapter will use a running example of a simple HTML editor. We have used a simple text editor for writing HTML because it's the only editor that gives you complete control over the contents and won't insert all those funny tags that most editors insert to ensure that your pages won't be compatible with everyone's browser. Still, it's nice to have some syntax highlighting, Content Assist, and other time-saving features, so we wrote a simple HTML editor for Eclipse.

Here is the skeleton of the HTMLEditor class, showing the customization entry points:

   public class HTMLEditor extends AbstractTextEditor {
      public HTMLEditor() {
         //install the source configuration
         setSourceViewerConfiguration(new HTMLConfiguration());
         //install the document provider
         setDocumentProvider(new HTMLDocumentProvider());
      }
      protected void createActions() {
         super.createActions();
         //... add other editor actions here
      }
   }

See Also:
---------

[FAQ\_What\_support\_is\_there\_for\_creating\_custom\_text_editors?](./FAQ_What_support_is_there_for_creating_custom_text_editors.md "FAQ What support is there for creating custom text editors?")

Go to **Platform Plug-in Developer Guide > Programmer's Guide >** Editors > Configuring a source viewer**.**

