

FAQ Where can I find a list of existing action group names?
===========================================================

Eclipse menus and toolbars are divided into groups to ensure that related actions appear together. These group names need to be clearly specified to allow plug-ins to contribute their actions to menus defined by other plug-ins. Group names can be specified either programmatically or declaratively in a plugin.xml file. For groups that are defined programmatically, the convention is to create an interface called I*ActionConstants containing constants for all the plug-in's group names. This serves to cement the group names as API, ensuring that they will stay consistent across releases of Eclipse. The base platform defines all its groups in IWorkbenchActionConstants, and the text infrastructure defines ITextEditorActionConstants. A similar pattern is used for other plug-ins, such as IJavaEditorActionConstants for the Java editor.

  
The other way to specify group names is in plugin.xml. Most plug-ins use this approach, making it fairly easy to track down the group names. Simply open the plugin.xml of the plug-in you want to contribute your action to and look for the definition of the menu element under an actionSet extension. These IDs are also treated as API, so they generally won't change from release to release.

  

See Also:
---------

[FAQ\_What\_is\_an\_action_set?](./FAQ_What_is_an_action_set.md "FAQ What is an action set?")

[FAQ\_How\_do\_I\_add\_actions\_to\_the\_main_menu?](./FAQ_How_do_I_add_actions_to_the_main_menu.md "FAQ How do I add actions to the main menu?")

