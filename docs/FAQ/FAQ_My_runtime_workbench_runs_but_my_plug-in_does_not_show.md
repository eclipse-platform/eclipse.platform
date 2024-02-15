

FAQ My runtime workbench runs, but my plug-in does not show. Why?
=================================================================

*   [FAQ How do I create a plug-in?](./FAQ_How_do_I_create_a_plug-in.md "FAQ How do I create a plug-in?") contains instructions for writing plug-ins in Eclipse.
*   [FAQ Why doesn't my plug-in build correctly?](./FAQ_Why_doesnt_my_plug-in_build_correctly.md "FAQ Why doesn't my plug-in build correctly?") tells you what to do if your plug-in has problems building.
*   [FAQ How do I run my plug-in in another instance of Eclipse?](./FAQ_How_do_I_run_my_plug-in_in_another_instance_of_Eclipse.md "FAQ How do I run my plug-in in another instance of Eclipse?") explains how to run a plug-in in another instance of Eclipse.

Despite all these instructions, in some cases, your plug-in builds fine and the runtime workbench launches but your plug-in still does not show. Various configuration-related things could be wrong.

*   The plug-in may not be selected in the launch configuration. See [FAQ How do I run my plug-in in another instance of Eclipse?](./FAQ_How_do_I_run_my_plug-in_in_another_instance_of_Eclipse.md "FAQ How do I run my plug-in in another instance of Eclipse?") for instructions on how to select plug-ins.
*   Your plug-in may rely on other plug-ins not enabled in your current launch configuration. Check the error log. See [FAQ Where can I find that elusive .log file?](./FAQ_Where_can_I_find_that_elusive_log_file.md "FAQ Where can I find that elusive .log file?") for messages referring to your plug-in.
*   If your plug-in contributes an action to the toolbar and it does not show or if a menu option does not appear, your plug-in may still be activated. The workbench does not automatically add toolbar and menu items to every perspective. Run **Window > Customize Perspective** and verify whether your contribution is enabled for the current perspective.
*   Your plug-in may not show because it is not yet needed. If all your plug-in does is contribute a view, you may need to show it explicitly by using **Window > Show View > Other...**. If your plug-in contributes an editor for a particular type, you may need to configure the file association; see **Window > Preferences > Workbench > File Associations**.
*   Your plug-in may throw an exception in its static initializer or in its plug-in class instance initializer. Again, consult the log file for error messages involving your plug-in.

See Also:
---------

[FAQ What causes my plug-in to build but not to load in a runtime workbench?](./FAQ_What_causes_my_plug-in_to_build_but_not_to_load_in_a_runtime_workbench.md "FAQ What causes my plug-in to build but not to load in a runtime workbench?")

