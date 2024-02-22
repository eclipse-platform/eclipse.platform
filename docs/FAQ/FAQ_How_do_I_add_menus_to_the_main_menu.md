

FAQ How do I add menus to the main menu?
========================================

Menus and submenus are added to the main menu by using the org.eclipse.ui.actionSets extension point. Here is an example of a top-level menu defined by the FAQ Examples plug-in:

      <menu
         label="FA&Q Examples"
         id="exampleMenu">
         <separator name="exampleGroup"/>
      </menu>

Each menu contains one or more separator elements that define _groups_ within that menu. The menu ID, along with the separator name, is used by actions contributing to that menu. If you want to create a submenu, you also need to define a path attribute that specifies what menu and group your menu should appear under. This path attribute has the same syntax as the menubarPath attribute on action definitions. To add a submenu to the menu defined earlier, you would add the following attribute:

      path="exampleMenu/exampleGroup"

To add a submenu to the **File** menu after the **New** submenu, the path would be:

      path="file/new.ext"

The syntax of menu paths is described in more detail in FAQ 223.

  

See Also:
---------

[FAQ\_What\_is\_an\_action_set?](./FAQ_What_is_an_action_set.md "FAQ What is an action set?")

[FAQ\_How\_do\_I\_add\_actions\_to\_the\_main_menu?](./FAQ_How_do_I_add_actions_to_the_main_menu.md "FAQ How do I add actions to the main menu?")

[FAQ\_Where\_can\_I\_find\_a\_list\_of\_existing\_action\_group_names?](./FAQ_Where_can_I_find_a_list_of_existing_action_group_names.md "FAQ Where can I find a list of existing action group names?")

