

FAQ How do I build menus and toolbars programmatically?
=======================================================

Menus and toolbars in JFace are based on two key interfaces: IContributionItem and IContributionManager. A contribution manager is simply an object that contains contribution items. The major types of contribution managers are menus, toolbars, and status lines. Contribution items represent any object that is logically contained within a menu or a toolbar, such as actions, submenus, and separators. These interfaces abstract away the differences between the contexts in which actions can appear. An action doesn't care whether it is invoked from a toolbar or a menu, and these interfaces help avoid unnecessary coupling between the items and the containers presenting them.

So, for each toolbar or menu, you need to create a contribution manager. For menus, including drop-down menus, context menus, and submenus, create an instance of MenuManager. For toolbars or cool bars, create an instance of ToolBarManager or CoolBarManager, respectively. The following snippet creates a top-level menu and a submenu, each with one action:

      IMenuManager mainMenu = ...;//get ref to main menu manager
      MenuManager menu1 = new MenuManager("Menu &1", "1");
      menu1.add(new Action("Action 1") {});
      mainMenu.add(menu1);
      MenuManager menu2 = new MenuManager("Menu &2", "2");
      menu2.add(new Action("Action 2") {});
      menu1.add(menu2);

See Also:
---------

*   [FAQ\_How\_do\_I\_make\_menus\_with\_dynamic\_contents?](./FAQ_How_do_I_make_menus_with_dynamic_contents.md "FAQ How do I make menus with dynamic contents?")
*   [FAQ\_What\_is\_the\_difference\_between\_a\_toolbar\_and\_a\_cool_bar?](./FAQ_What_is_the_difference_between_a_toolbar_and_a_cool_bar.md "FAQ What is the difference between a toolbar and a cool bar?")
*   [Menu\_Contributions/Populating\_a\_dynamic\_submenu](https://github.com/eclipse-platform/eclipse.platform.ui/blob/master/docs/Menu_Contributions/Populating_a_dynamic_submenu.md "Menu Contributions/Populating a dynamic submenu")

