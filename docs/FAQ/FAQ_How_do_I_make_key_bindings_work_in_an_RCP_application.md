

FAQ How do I make key bindings work in an RCP application?
==========================================================

When actions are contributed via the actionSets extension point, key bindings are configured by associating the action with a declarative command. In this case, no code is required to hook the action to the key binding. However, if you programmatically create actions in an RCP application, you have to register actions yourself. This requires two steps. First, you need to specify the command ID for your action. If you are using built-in actions from an action factory, they usually have the command ID already set. If you create your own action, as a subclass of Action, you need to set the command ID yourself by calling the setActionDefinitionId method inherited from Action. Typically this is done from your action's constructor.

Now that your action is linked to a command, you need to register the action with the platform. You should do this the first time the platform calls your implementation of WorkbenchAdvisor.fillActionBars:

      public void fillActionBars(IWorkbenchWindow window,
         IActionBarConfigurer configurer, int flags) {
         ...
         if (maximizeAction == null) {
            maximizeAction = ActionFactory.MAXIMIZE.create(window);
            configurer.registerGlobalAction(maximizeAction);
         }
         menu.add(maximizeAction);
      }

The method registerGlobalAction will let the platform know that your action exists. When the key binding is invoked by the user, it will now be able locate and run your action.

See Also:
---------

*   [FAQ What is the difference between a command and an action?](./FAQ_What_is_the_difference_between_a_command_and_an_action.md "FAQ What is the difference between a command and an action?")
*   [FAQ How do I associate an action with a command?](./FAQ_How_do_I_associate_an_action_with_a_command.md "FAQ How do I associate an action with a command?")
*   [FAQ How do I provide a keyboard shortcut for my action?](./FAQ_How_do_I_provide_a_keyboard_shortcut_for_my_action.md "FAQ How do I provide a keyboard shortcut for my action?")
*   [FAQ What is the difference between a command and an action?](./FAQ_What_is_the_difference_between_a_command_and_an_action.md "FAQ What is the difference between a command and an action?")

