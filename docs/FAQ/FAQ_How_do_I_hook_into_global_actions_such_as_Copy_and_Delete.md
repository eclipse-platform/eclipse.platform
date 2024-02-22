

FAQ How do I hook into global actions, such as Copy and Delete?
===============================================================

Certain standard toolbar and menu entries can be shared among several views and editors. These actions are called either _global_ or _retargetable_ actions and include such common tools as undo/redo, cut/copy/paste, print, find, delete, and more. Each view or editor is allowed to contribute a handler for these actions; when a new part becomes active, its handler takes control of that action.

A view typically registers its global action handlers in the createPartControl method:

      IActionBars actionBars= getViewSite().getActionBars();
      actionBars.setGlobalActionHandler(
         ActionFactory.COPY.getId(),
         copyAction);	

You have to do this only once for each view that is created. The platform remembers your action handler and retargets the action each time the view becomes active. To unregister from a global action, simply invoke setGlobalActionHandler again and pass in a null value for the handler.

  
The IWorkbenchActionConstants interface in the org.eclipse.ui package contains a complete list of global actions. Look for constants in this interface with a comment saying Global action. In Eclipse 3.0, you can also look at the ActionFactory and IDEActionFactory classes, which define factory objects for creating a variety of common actions.

  

See Also
--------

[FAQ How do I enable global actions such as Cut, Paste, and Print in my editor?](./FAQ_How_do_I_enable_global_actions_such_as_Cut_Paste_and_Print_in_my_editor.md "FAQ How do I enable global actions such as Cut, Paste, and Print in my editor?")

[FAQ How do I add actions to the global toolbar?](./FAQ_How_do_I_add_actions_to_the_global_toolbar.md "FAQ How do I add actions to the global toolbar?")

