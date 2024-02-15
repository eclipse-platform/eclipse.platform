FAQ/Language integration/phase 4: What are the finishing touches?
=================================================================

After following the steps in phases 1 to 3, you have successfully written a compiler, a builder, a DOM, and an integrated editor. What remains are a few finishing touches:

  

*   _Add a project wizard_. Your language may benefit from similar wizards as provided by JDT to create projects, classes, and interfaces.

See [FAQ What wizards do I define for my own language?](./FAQ_What_wizards_do_I_define_for_my_own_language.md "FAQ What wizards do I define for my own language?")

  

*   _Declare a project nature_. Natures can be used to facilitate the enablement of builders on certain projects.

See [FAQ When does my language need its own nature?](./FAQ_When_does_my_language_need_its_own_nature.md "FAQ When does my language need its own nature?")

  

*   _Declare a perspective_. Perspectives can be used to organize views and editors into a cohesive, collaborative set of tools.

See [FAQ When does my language need its own perspective?](./FAQ_When_does_my_language_need_its_own_perspective.md "FAQ When does my language need its own perspective?")

  

*   _Add documentation_. Traditionally this is done as one of the last steps in any agile software project.

Eclipse has support for adding documentation to a set of plug-ins through its help system, accessed with **Help > Help Contents...**. Context-sensitive help can be activated by using F1. For more information about how to add documentation and help for your language, see [FAQ How do I add documentation and help for my own language?](./FAQ_How_do_I_add_documentation_and_help_for_my_own_language.md "FAQ How do I add documentation and help for my own language?")

  

*   _Add source level debugging support_. Implementing support for source-level debugging is arguably the most difficult to implement, even in the highly configurable Eclipse.

See [FAQ How do I support source-level debugging for my own language?](./FAQ_How_do_I_support_source-level_debugging_for_my_own_language.md "FAQ How do I support source-level debugging for my own language?") for a discussion.

  

Congratulations. You followed all steps outlined in the four phases of language integration and are to be commended for getting this far. Writing an IDE in Eclipse is the most elaborate and wide-ranging exercise to perform on top of Eclipse.

  

