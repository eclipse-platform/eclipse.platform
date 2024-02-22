

FAQ How do I customize the menus in an RCP application?
=======================================================

Your RCP application must specify what menus and actions, if any, to include by default in the main workbench window menu bar. This is done by overriding the WorkbenchAdvisor fillActionBars method. Note that although other plug-ins are always free to create their own menus, it is common for plug-ins to assume the existence of some basic menus. You are responsible for creating the menus that you expect all plug-ins to your application to contribute to.

  
Here is a simple example of an advisor that creates two menus-**Window** and **Help**-and adds a single action to each:

      public void fillActionBars(IWorkbenchWindow window,
         IActionBarConfigurer configurer, int flags) {
         if ((flags & FILL_MENU_BAR) == 0)
            return;
         IMenuManager mainMenu = configurer.getMenuManager();
         MenuManager windowMenu = new MenuManager("&Window", 
            IWorkbenchActionConstants.M_WINDOW);
         mainMenu.add(windowMenu);
         windowMenu.add(ActionFactory.MAXIMIZE.create(window));
         MenuManager helpMenu = new MenuManager("&Help", 
            IWorkbenchActionConstants.M_HELP);
         mainMenu.add(helpMenu);
         helpMenu.add(new AboutAction());
      }

Note how the menu IDs are taken from IWorkbenchActionConstants. It is important to use the standard menu IDs as plug-ins contributing to the actionSets extension point will be expecting these standard IDs. The action added to the **Window** menu is taken from the standard set of actions available from org.eclipse.ui.actions.ActionFactory. You will find many of the standard perspective, view, and editor manipulation actions here. The AboutAction in this snippet is a simple custom action that displays program information and credits, conventionally added by most applications at the bottom of the **Help** menu.

  
For simplicity, this snippet creates new actions each time fillActionBars is called. In a real application, you should create the actions only once and return the cached instances whenever this method is called. Because actions often add themselves as selection or part-change listeners, creating multiple action instances would introduce performance problems. A common place to store action instances is in the data cache provided by IWorkbenchWindowConfigurer. Because each workbench window has its own configurer instance, this is an ideal place to store state specific to a given window. You can use a convenience method such as the following to lazily initialize and store your created actions:

      //configurer is provided by initialize method
      private IWorkbenchConfigurer configurer = ...;
      private static final String MENU_ACTIONS = &menu.actions&;
      private IAction[] getMenuActions(IWorkbenchWindow window) {
         IWorkbenchWindowConfigurer wwc =
            configurer.getWindowConfigurer(window);
         IAction[] actions = (IAction[]) wwc.getData(MENU_ACTIONS);
         if (actions == null) {
            IAction max = ActionFactory.MAXIMIZE.create(window);
            actions = new IAction[] {max};
            wwc.setData(MENU_ACTIONS, actions);
         }
         return actions;
      }

  

It is common practice to factor out action management code into a helper class and then store an instance of this helper class in the window configurer's cache.

Deprecation warning
-------------------

Note that WorkbenchAdvisor fillActionBars is deprecated and ActionBarAdvisor.fillActionBars(int) should be used instead.

See Also:
---------

[FAQ How do I build menus and toolbars programmatically?](./FAQ_How_do_I_build_menus_and_toolbars_programmatically.md "FAQ How do I build menus and toolbars programmatically?")

