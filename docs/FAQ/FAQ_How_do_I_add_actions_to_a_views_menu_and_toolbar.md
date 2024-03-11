

FAQ How do I add actions to a view's menu and toolbar?
======================================================

Each view has a drop-down menu in two locations:

*   _under the icon on the view's tab item_.

This menu contains layout and view-manipulation actions. You don't have any control over this menu; its actions are all added by the platform.

  

*   _in the view's toolbar_.

The drop-down menu on the right-hand side, a small downward-pointing triangle, is controlled by your view. This menu will exist only if you add actions to it.

  

Actions are added to the menu and toolbar by using the IActionBars interface. This interface is used to access the standard JFace menu and toolbar manager objects used for creating menus throughout Eclipse. The following code, usually invoked from the view's createPartControl method, adds a single action to the view's menu and toolbar:

      Action action = ...;
      IActionBars actionBars = getViewSite().getActionBars();
      IMenuManager dropDownMenu = actionBars.getMenuManager();
      IToolBarManager toolBar = actionBars.getToolBarManager();
      dropDownMenu.add(action);
      toolBar.add(action);
      actionBars.updateActionBars();

  

See Also:
---------

[FAQ How do I build menus and toolbars programmatically?](./FAQ_How_do_I_build_menus_and_toolbars_programmatically.md "FAQ How do I build menus and toolbars programmatically?")

