FAQ/Language integration/phase 3: How do I edit programs?
=========================================================

After creating a compiler, a builder, and a DOM, writing an editor is a snap. To write an editor for a particular programming language, a few steps can be distinguished, all relying heavily on the existence of a DOM.

*   _Implement a language-specific editor_.

The JDT places the bar high for any subsequent language implementers. No matter how fast the compiler is and how well the build process is integrated, if your language has to be edited in the default text editor, you fail to even get close to being worthy of comparison to JDT. Writing an editor is not difficult. Many examples exist. The platform wizard has one for an XML editor. The examples shipped with Eclipse show a simplified Java editor. This book has a sample that shows how to write an HTML editor. For more details, see [FAQ How do I write an editor for my own language?](./FAQ_How_do_I_write_an_editor_for_my_own_language.md "FAQ How do I write an editor for my own language?")

*   _Add Content Assist_. (see [FAQ How do I add Content Assist to my language editor?](./FAQ_How_do_I_add_Content_Assist_to_my_language_editor.md "FAQ How do I add Content Assist to my language editor?"))

The DOM, developed in phase 2, allows us to navigate the source code, analyze it, present it in multiple modes, and manipulate its structure, a process also known as _refactoring_. Content Assist uses the DOM to figure out all the possible context-sensitive continuations for a given input. Quick Fixes know how to solve a given compilation error. Refactoring relies on the DOM to find all call sites for a given method before we can change its name. An Outline view uses the DOM to show the structure of the code in a hierarchical summary format.

*   _Add Quick Fixes_ (see [FAQ How do I implement Quick Fixes for my own language?](./FAQ_How_do_I_implement_Quick_Fixes_for_my_own_language.md "FAQ How do I implement Quick Fixes for my own language?")

After compilation errors have been detected, suggest how to fix the problem. How would you reason about code without an underlying model?

*   _Add refactoring_ (see [FAQ How do I support refactoring for my own language?](./FAQ_How_do_I_support_refactoring_for_my_own_language.md "FAQ How do I support refactoring for my own language?"))

Implement operations on source code to restructure program constructs, following the semantics of your language. Again, without a model of the underlying language, this is a daunting, error-prone task.

*   _Add an Outline view_ (see [FAQ How do I create an Outline view for my own language editor?](./FAQ_How_do_I_create_an_Outline_view_for_my_own_language_editor.md "FAQ How do I create an Outline view for my own language editor?"))

The Outline view presents a summary of the structure of a particular program. Using the same compiler and/or DOM saves a lot of time developing your language IDE.
  

After completing your editor, you are ready to enter the Holy Grail of language IDEs; see [FAQ Language integration phase 4: What are the finishing touches?](./FAQ_Language_integration_phase_4_What_are_the_finishing_touches.md "FAQ Language integration phase 4: What are the finishing touches?")

