

FAQ What wizards do I define for my own language?
=================================================

This depends on your language. For instance, the PDE offers wizards for creating plug-ins, features, plug-in fragments, and update sites. In addition, the PDE provides support for converting something existing into a form it can work with, such as converting a regular project to a plug-in project.

In the case of Java, the JDT offers wizards for the obvious things-Java projects, packages, classes, and interfaces-as well as for less obvious ones such as a wizard for creating a scrapbook page and a source folder. The CDT offers wizards for generating a C++ class and for creating either a standard make file project or a managed make project for C or C++. Furthermore, the CDT has a wizard for converting a normal project to a C/C++ project.

If we look at eScript, the only appropriate wizard type seems to be the creation of an eScript file, where the user would choose whether the generated code should include the definition of a feature and an update site. An extra wizard page could be added to generate code to implement plug-ins that contribute a view, editor, and so on.

When certain wizards are used frequently, consider showing them in the toolbar by contributing an action set.

For instructions on writing wizards, look at **Help > Platform Plug-in Developer Guide > Programmer's Guide > Dialogs and Wizards > Wizards**.

See Also:
---------

*   [FAQ\_What\_is\_a\_wizard?](./FAQ_What_is_a_wizard.md "FAQ What is a wizard?")
*   [FAQ\_When\_does\_my\_language\_need\_its\_own\_nature?](./FAQ_When_does_my_language_need_its_own_nature.md "FAQ When does my language need its own nature?")

