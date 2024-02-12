

FAQ How do I associate an action with a command?
================================================

Actions are associated with commands in various ways depending on how the actions are defined. For actions contributed via the actionSets extension point, the association with a command is done directly in the action definition. The definitionId attribute of the action element must match the ID of the command it is associated with:

      <actionSet ...>
         <action
            definitionId="org.eclipse.faq.sampleCommand"
            ...>
         </action>
      </actionSet>
      <command
         id="org.eclipse.faq.sampleCommand"
         ...>
      </command>

For actions created programmatically, associating the action with a command is a two-step process. As with declarative actions, the first step is to set the action's definition ID to match the ID of the command. The command must still be defined declaratively, using the command extension point. The definition ID is set by calling Action.setDefinitionId. The second step is to register the action with the platform, using the key-binding service. This service can be accessed from the IWorkbenchPartSite, which is accessible to both views and editors. Here is an example of these steps for an action in a view:

      action.setActionDefinitionId("some.unique.id");
      view.getSite().getKeyBindingService().registerAction(action);

See Also:
---------

*   [FAQ How do I make key bindings work in an RCP application?](./FAQ_How_do_I_make_key_bindings_work_in_an_RCP_application.md "FAQ How do I make key bindings work in an RCP application?")
*   [FAQ What is the difference between a command and an action?](./FAQ_What_is_the_difference_between_a_command_and_an_action.md "FAQ What is the difference between a command and an action?")

