

FAQ How do I use the text document model?
=========================================

The underlying model behind text editors is represented by the interface IDocument and its default implementation, Document, both of which are declared in the org.eclipse.jface.text package. You can use documents to manipulate text inside and outside a text editor. Documents are created with a simple constructor that optionally takes a string representing the initial input. The document contents can be obtained and replaced by using get() and set(String). The document model has a powerful search method and several methods for querying or replacing portions of the document. The following example uses a document to implement search and replace:

      String searchAndReplace(String input, String search, 
      String replace) throws BadLocationException {
         Document doc = new Document(input);
         int offset = 0;
         while (offset < doc.getLength()) {
            offset = doc.search(offset, search, true, true, true);
            if (offset < 0)
               break;
            doc.replace(offset, search.length(), replace);
            offset += replace.length();
         }
         return doc.get();
      }

  
This example only scratches the surface of the capabilities of the IDocument model. Documents also provide change notification, mapping between line numbers and character offsets, partitions, and much more. Other FAQs in this chapter dig into some of these concepts in more detail.

  

See Also:
---------

[FAQ\_What\_is\_a\_document_partition?](./FAQ_What_is_a_document_partition.md "FAQ What is a document partition?")

[FAQ\_How\_do\_I\_insert\_text\_in\_the\_active\_text\_editor?](./FAQ_How_do_I_insert_text_in_the_active_text_editor.md "FAQ How do I insert text in the active text editor?")

Go to **Platform Plug-in Developer Guide > Programmer's Guide >Editors > Documents and partitions** in Eclipse help, or at [help.eclipse.org](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/guide/editors_documents.htm).

