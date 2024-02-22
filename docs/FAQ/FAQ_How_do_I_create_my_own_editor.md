

FAQ How do I create my own editor?
==================================

All editors start by making a subclass of EditorPart from the org.eclipse.ui.part package. At this basic level, editors are very generic. They take an input object that implements IEditorInput, they know how to draw themselves by implementing the createPartControl method, and they may know how to respond to a request to save their contents. To create a bare-bones editor, you have to implement only a small handful of methods. Figure 11.1 shows a working editor that displays a simple label.
  
Some required methods, declared abstract in EditorPart, have been omitted from this snippet, but the editor will work if their implementations are empty:

      public class MinimalEditor extends EditorPart {
         private Label contents;
         public void createPartControl(Composite parent) {
            contents = new Label(parent, SWT.NONE);
            contents.setText("Minimal Editor");
         }
         public void init(IEditorSite site, IEditorInput input) {
            setSite(site);
            setInput(input);
         }
         public void setFocus() {
            if (contents != null)
               contents.setFocus();
         }
      }

The plug-in manifest entry for defining this editor is as follows:

      <editor
         name="Minimal Editor"
         extensions="min"
         icon="icons/sample.gif"
         class="org.eclipse.faq.examples.editor.MinimalEditor"
         id="org.eclipse.faq.examples.editor.MinimalEditor">
      </editor>

  
The extensions attribute describes what file types this editor will automatically be associated with. In this case, the editor will be associated with files ending in min. An editor can instead be associated with a particular file name by replacing the extensions attribute with a filenames attribute. Either of these attributes can specify a comma-separated list for associating an editor with multiple file name extensions and/or file names. A user can always choose to associate an editor with a different file name or extension from the **Workbench > File Associations** preference page.

  

See Also:
---------

[FAQ\_How\_do\_I\_get\_started\_with\_creating\_a\_custom\_text_editor?](./FAQ_How_do_I_get_started_with_creating_a_custom_text_editor.md "FAQ How do I get started with creating a custom text editor?")

[FAQ\_How\_do\_I\_write\_an\_editor\_for\_my\_own\_language?](./FAQ_How_do_I_write_an_editor_for_my_own_language.md "FAQ How do I write an editor for my own language?")

