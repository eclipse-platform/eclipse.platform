

FAQ How do I make menus with dynamic contents?
==============================================

By default, menu managers in JFace are static; that is, contributions are added once when the view is created, and they remain unchanged each time the view is opened. Individual actions may enable or disable themselves, based on the current context, but the menu itself remains stable.

If you want the contents of your menus to change every time the menu is opened, you should make your menu manager dynamic. When the menu manager is created, make it dynamic by calling setRemoveAllWhenShown:

      IMenuManager menu = new MenuManager("Name");
      menu.add(new Action("never shown entry"){}); //needed if it's a submenu
      menu.setRemoveAllWhenShown(true);

By setting this flag, the menu manager will remove all actions and submenus from the menu every time the menu is about to be shown. If the menu is a submenu you **must** include a fake item **or the dynamic menu will not be shown** (see [Bug #149890](https://bugs.eclipse.org/bugs/show_bug.cgi?id=149890)). Next, you need to install on the menu a listener that will add your actions:

      IMenuListener listener = new IMenuListener() {
         public void menuAboutToShow(IMenuManager m) {
         if (daytime) {
            m.add(workAction);
            m.add(playAction);
         } else {
            m.add(sleepAction);
         }
         }
      };
      menu.addMenuListener(listener);

This menu will now have different actions, depending on the value of the daytime variable.

It is possible to make a menu that is partially dynamic, with some actions added at creation time and some actions added or removed by a menu listener when the menu is opened. However, it is generally not worth the added complexity. Note that even for dynamically contributed actions, you should retain the same action instances and recontribute them each time the menu opens. Creating action instances from scratch every time the menu opens is not generally a good idea as actions often install themselves as listeners or perform other nontrival initialization.

