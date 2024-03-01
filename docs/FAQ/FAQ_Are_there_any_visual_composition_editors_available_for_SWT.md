FAQ Are there any visual composition editors available for SWT?
===============================================================

Several free and commercial products provide visual composition editors, or GUI builders, for SWT. 
These tools are especially appealing to people who are not yet skilled in all the intricacies of the SWT layout mechanisms and do not yet know what kinds of widgets are available to choose from.

The UI for Eclipse is written manually in SWT, using an additional UI framework called JFace to take care of some of the repetitive aspects of writing UIs. Furthermore, when defining a new dialog, the developers often use the Monkey see, monkey do rule: They first find one that is close to the intended result; then, the new UI is cloned from the inspiration source and modified until it fits the needs of the new application.

Developers want to prototype and develop user interfaces with SWT. 
Visual builders help less experienced developers by eliminating most of the guesswork from the UI design labor. 
Widgets can be selected from a panel, and attributes can be chosen and assigned values from a limited set of options.
The most successful builders offer fully synchronized views of the UI being developed and the generated source code to implement the UI.

Multiple commercial builders are available and the main free  GUI builders is [WindowBuilder](https://github.com/eclipse-windowbuilder/windowbuilder).


See Also:
---------

*   [FAQ What open source projects are based on Eclipse?](./FAQ_What_open_source_projects_are_based_on_Eclipse.md "FAQ What open source projects are based on Eclipse?")

