

FAQ How do I add actions to the main menu?
==========================================

Contents
--------

*   [1 Overview](#Overview)
*   [2 Common Menupaths](#Common-Menupaths)
    *   [2.1 Standard Menus](#Standard-Menus)
    *   [2.2 Standard group for adding top level menus](#Standard-group-for-adding-top-level-menus)
    *   [2.3 Standard file actions](#Standard-file-actions)
    *   [2.4 Most Recently Used File group](#Most-Recently-Used-File-group)
    *   [2.5 Standard edit actions](#Standard-edit-actions)
    *   [2.6 Standard help actions](#Standard-help-actions)
*   [3 See Also](#See-Also)

Overview
--------

As with menus and toolbar buttons, menu actions are added to the main menu by using the org.eclipse.ui.actionSets extension point. The FAQ Examples plug-in has many actions that are contributed to the menu in this way. Here is a sample action definition (a subelement of the actionSets element):

      <action
         label="Open Error &Dialog"
         class=
         "org.eclipse.faq.examples.actions.OpenErrorDialogAction"
         menubarPath="exampleMenu/exampleGroup">
      </action>

The class attribute specifies the fully qualified path of the Action class, which must implement IWorkbenchWindowActionDelegate. When the action is selected by the user, the action's run method will be invoked.

  
The menubarPath attribute specifies the location of the action within the menus. This path is one of the greatest sources of confusion-and one of the biggest FAQs-for new users of action sets, so it is worth explaining it here in detail. The path has two parts, both of which are required. Everything up to the last slash character (/) represents the path of the menu that the action will belong to. For top-level menus, this is a simple string.

The IWorkbenchActionConstants interface contains constants for the standard top-level menu names. For example, file is the path of the **File** menu, and window is the path of the **Window** menu. For menus defined in other plug-ins, consult the plugin.xml file to see the ID of their menus.

When contributing to a submenu, the menu path will be a slash-delimited string containing the IDs of each menu in the hierarchy. The FAQ Examples plug-in defines a top-level menu with ID exampleMenu, and a submenu below this with ID exampleFile. The menubarPath of an action contributed to this submenu would therefore start with exampleMenu/exampleFile.

  
The final part of the menubarPath attribute-after the last slash-is the group name. All menus are divided into groups as a means of organizing the actions within them. When contributing an action, you must specify the name of the group your action belongs to within that menu. Each action can belong to only one group in a single menu. Once again, the standard group names used in the top-level menus are defined in IWorkbenchActionConstants.

Some examples of menubarPath attributes will help to illustrate how they are used. Here is the path for an action in the import/export group within the top-level **File** menu:

      menubarPath="file/import.ext"

An action contributed to the group of **Show...** actions in the **Navigate** menu would have the following path:

      menubarPath="navigate/show.ext"

Finally, an action contributed to the **Editor** submenu in the **FAQ Examples** menu would have this path:

      menubarPath="exampleMenu/editorMenu/editorGroup"

Note that the group name is required even for menus that have only one group.

  
Menu actions have many more optional attributes, including those for specifying the action's visibility and enablement. Consult the extension point documentation for complete details.

Common Menupaths
----------------

Here is a listing of common standard menupaths from class org.eclipse.ui.IWorkbenchActionConstants.

### Standard Menus

*   Standard File menu -- M_FILE = "file"
*   Standard Edit menu -- M_EDIT = "edit"
*   Standard Window menu -- M_WINDOW = "window"
*   Standard Help menu -- M_HELP = "help"

### Standard group for adding top level menus

*   Standard addition group -- MB_ADDITIONS = "additions"

### Standard file actions

*   Group for start of menu -- FILE_START = "fileStart"
*   Group for end of menu -- FILE_END = "fileEnd"
*   Group for extra New-like actions -- NEW_EXT = "new.ext"
*   Group for extra Close-like actions -- CLOSE_EXT = "close.ext"
*   Group for extra Save-like actions -- SAVE_EXT = "save.ext"
*   Group for extra Print-like actions -- PRINT_EXT = "print.ext"
*   Group for extra Import-like actions -- IMPORT_EXT = "import.ext"

### Most Recently Used File group

*   Group for most recently used file -- MRU = "mru"

### Standard edit actions

*   Group for start of menu -- EDIT_START = "editStart"
*   Group for end of menu -- EDIT_END = "editEnd"

### Standard help actions

*   Group for start of menu -- HELP_START = "helpStart"
*   Group for end of menu -- HELP_END = "helpEnd"

See Also
--------

[FAQ What is an action set?](./FAQ_What_is_an_action_set.md "FAQ What is an action set?")

[FAQ Where can I find a list of existing action group names?](./FAQ_Where_can_I_find_a_list_of_existing_action_group_names.md "FAQ Where can I find a list of existing action group names?")

Eclipse online article Contributing Actions to the Eclipse Workbench

