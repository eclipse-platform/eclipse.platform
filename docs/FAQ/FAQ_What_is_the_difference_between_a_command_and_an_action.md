

FAQ What is the difference between a command and an action?
===========================================================

Since you have come this far, you probably already understand that Actions and Commands basically do the same thing: They cause a certain piece of code to be executed. They are triggered, mainly, from artifacts within the user interface. These artifacts can be an icon in a (tool)bar, a menu item or a certain key combination.

The action framework is proven, tightly integrated and fairly easy to program. So, why change?

Contents
--------

*   [1 Actions](#Actions)
*   [2 Commands](#Commands)
*   [3 References](#References)
*   [4 See Also:](#See-Also:)

Actions
-------

The main concern with Actions is that the manifestation and the code is all stored in the Action. Although there is some separation in Action Delegates, they are still connected to the underlying action. Selection events are passed to Actions so that they can change their enabled state (programmatically) based on the current selection. This is not very elegant. Also to place an action on a certain workbench part you have to use several extension points :

*   org.eclipse.ui.viewActions
*   org.eclipse.ui.popupMenus
*   org.eclipse.ui.editorActions

Commands
--------

Commands pretty much solve all these issues. The basic idea is that the Command is just the abstract idea of some code to be executed. The actual handling of the code is done by, well, handlers. Handlers are activated by a certain state of the workbench. This state is queried by the platform core expressions. This means that we only need one global _Save_ command which behaves differently based on which handler is currently active. Although this specific Command could also be retargeted by a global action, this still has to be done programmatically and not declaratively. To place a Command on a certain workbench part (including the trim area) you have to use only one extension point:

*   org.eclipse.ui.menus

Besides this, Handlers can be activated by a powerful expression syntax in the manifest. This means less code and more declarations which will lead to a smoother running workbench or RCP application.

If you look in the **General > Keys** preference page, you will see a list of all commands known to the platform, including what context (the "When" column) and configuration (the "Scheme" drop-down list?) they belong to. Key bindings are hooked to commands, and then commands are hooked to handlers. This extra level of indirection allows for added flexibility in the implementation. The user can change key bindings for a command without the associated handlers knowing about it, and the handler for a command can be dynamically changed in different circumstances.

References
----------

*   [Platform Command Framework](https://github.com/eclipse-platform/eclipse.platform.ui/blob/master/docs/PlatformCommandFramework.md)
*   [Platform Expression Framework](https://github.com/eclipse-platform/eclipse.platform.ui/blob/master/docs/Platform_Expression_Framework.md)
*   [Core Expressions](https://github.com/eclipse-platform/eclipse.platform.ui/blob/master/docs/Command_Core_Expressions.md)
*   [Menu Contributions](https://github.com/eclipse-platform/eclipse.platform.ui/blob/master/docs/Menu_Contributions.md)
*   [Eclipse Workbench Guide](https://help.eclipse.org/help33/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/workbench.htm)
*   [bug 223445 discusses references to an upcoming article](https://bugs.eclipse.org/bugs/show_bug.cgi?id=223445)

See Also:
---------

*   [FAQ How do I make key bindings work in an RCP application?](./FAQ_How_do_I_make_key_bindings_work_in_an_RCP_application.md "FAQ How do I make key bindings work in an RCP application?")
*   [FAQ How do I associate an action with a command?](./FAQ_How_do_I_associate_an_action_with_a_command.md "FAQ How do I associate an action with a command?")

