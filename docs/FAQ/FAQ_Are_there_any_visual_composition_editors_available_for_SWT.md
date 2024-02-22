

FAQ Are there any visual composition editors available for SWT?
===============================================================

Several free and commercial products provide visual composition editors, or GUI builders, for SWT. These tools are especially appealing to people who are not yet skilled in all the intricacies of the SWT layout mechanisms and do not yet know what kinds of widgets are available to choose from.

After doing an informal poll, we discovered that none of the respondents in the Eclipse development team uses a visual builder to implement Eclipse. The UI for Eclipse is written manually in SWT, using an additional UI framework called JFace to take care of some of the repetitive aspects of writing UIs. Furthermore, when defining a new dialog, the developers often use the Monkey see, monkey do rule: They first find one that is close to the intended result; then, the new UI is cloned from the inspiration source and modified until it fits the needs of the new application.

With the growing popularity of SWT, more and more developers want to prototype and develop user interfaces with SWT. Visual builders help less experienced developers by eliminating most of the guesswork from the UI design labor. Widgets can be selected from a panel, and attributes can be chosen and assigned values from a limited set of options. The most successful builders offer fully synchronized views of the UI being developed and the generated source code to implement the UI.

Visual builders have been a long time coming for SWT, but a number of free and commercial GUI builders are finally available.

Free Plug-ins
-------------

*   [_The Eclipse Visual Editor Project_](https://wiki.eclipse.org/Visual_Editor_Project). The goal of this Eclipse project is to build a framework for creating Eclipse-based GUI builders. This project follows the general Eclipse philosophy of creating a platform- and language-independent framework, with language- and platform-specific layers on top. This project provides GUI builders for SWT/RCP and Swing applications.

*   _V4ALL Assisi GUI-Builder_. This SourceForge GUI builder project targets both SWT and Swing. So far, it is the work of a single developer, and there does not appear to be much activity on it.

*   [_JellySWT_](http://jakarta.apache.org/commons/jelly/jellyswt.html). Jelly is a scripting engine that uses XML as its scripting language. The goal of JellySWT is to allow you to describe a UI by using Jelly script and then have it generate the Java code automatically. The idea is that it takes care of the tedious layout code for you. This isn't really a visual composition editor, but it is a GUI builder of sorts.

Commercial Plug-ins
-------------------

*   [_SWT designer_](http://www.swt-designer.com). This commercial plug-in to Eclipse, which targets only SWT, is fairly new as a commercial product but is based on an open source project that has been around for a while and has a strong following.

See Also:
---------

*   [FAQ What open source projects are based on Eclipse?](./FAQ_What_open_source_projects_are_based_on_Eclipse.md "FAQ What open source projects are based on Eclipse?")

