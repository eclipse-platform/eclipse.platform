

FAQ How do I implement a DOM for my language?
=============================================

A DOM represents the structure of your programming language. Its design and implementation are dependent of the target language and follow a few simple guidelines:

*   The DOM is hierarchical in nature and directly represents concrete elements in the program it represents (such as the program itself and its functions, declarations, and statements).
*   A DOM is used for defining context for Content Assist (see [FAQ How do I add Content Assist to my language editor?](./FAQ_How_do_I_add_Content_Assist_to_my_language_editor.md "FAQ How do I add Content Assist to my language editor?").
*   A DOM is useful for generating text hovers (see [FAQ How do I add hover support to my text editor?](./FAQ_How_do_I_add_hover_support_to_my_text_editor.md "FAQ How do I add hover support to my text editor?").
*   Creating outline views without a DOM is difficult (see [FAQ How do I create an Outline view for my own language editor?](./FAQ_How_do_I_create_an_Outline_view_for_my_own_language_editor.md "FAQ How do I create an Outline view for my own language editor?").
*   A DOM is essential in architecting and implementing support for refactoring (see [FAQ How do I support refactoring for my own language?](./FAQ_How_do_I_support_refactoring_for_my_own_language.md "FAQ How do I support refactoring for my own language?").
*   A program may be represented with various DOMs. In the case of eScript we have a DOM for describing the program structure and a second DOM for the method bodies.
*   A DOM is implemented as a data structure with access API. In the case of eScript, we move Content Assist and text-hover support into the DOM nodes. This makes handling those in the editor very easy.
*   A DOM can be generated in two modes:
    *   The same compiler that also compiles source code can save its abstract syntax tree (AST) and expose it for use by the editor. Using an AST is a pretty standard way to implement a computer language, and piggybacking on that infrastructure makes life a lot easier when writing your editor. This is the way the eScript editor works.
    *   If the underlying AST is not accessible or is to too fine-grained for use in an editor, you may decide to implement a lightweight parser and generate a DOM more efficiently. This is the way the JDT implements its Java model.

The inheritance hierarchy for the DOM  defines the following fields:

      int startOffset, endOffset; // source positions
      Hashtable attributes;       // things like ID, label, ...
      ArrayList children;         // children of this element
      Element parent;             // the owner of this element
      String hoverHelp;           // cached value of hover help
      ...more fields....

The subclasses of Element implement useful methods:

      public String getAttributeValue(String name)
      public String getHoverHelp()
      public void getContentProposals(...., ArrayList result)   

For instance, the getHoverHelp method easily allows us to use the DOM to find the element at a given offset and then ask it for what hover help is appropriate.

  


  

See Also:
---------

[FAQ How can I ensure that my model is scalable?](./FAQ_How_can_I_ensure_that_my_model_is_scalable.md "FAQ How can I ensure that my model is scalable?")

