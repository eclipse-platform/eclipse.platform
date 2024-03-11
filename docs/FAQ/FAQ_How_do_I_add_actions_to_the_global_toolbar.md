

FAQ How do I add actions to the toolbar?
========================================

Actions are added to the workbench window's toolbar by using the org.eclipse.ui.actionSets extension point. Here is a sample action element that contributes an action to the workbench window toolbar:

      <action
         class="org.eclipse.faq.examples.actions.ToolBarAction"
         toolbarPath="Normal/exampleGroup"
         icon="icons/sample.gif"
         tooltip="Sample toolbar action">
      </action>

  
The class attribute is the fully qualified name of the action that will be run when the toolbar button is clicked. This class must implement the interface IWorkbenchWindowActionDelegate. The toolbarPath attribute has two segments-the toolbar ID and the group ID-separated by a slash (/) character. The toolbar ID is used to indicate which toolbar the action belongs to. This value isn't currently used because the platform defines only one toolbar, but the convention is to use the string Normal to represent the default toolbar. The group ID is used to place similar actions together. All actions with the same group ID will be placed in a fixed group in the toolbar. The string in the tooltip attribute is shown when the user hovers over the toolbar button.

  
You can specify many more attributes on your action, including criteria for when your action should be visible and when it should be enabled. However, the four attributes shown earlier are the minimum set you need for a toolbar action. Some of the other action attributes are discussed in other FAQs.

  

See Also
--------

[FAQ\_What\_is\_an\_action_set?](./FAQ_What_is_an_action_set.md "FAQ What is an action set?")

**Platform Plug-in Developer Guide**, under **Reference >** Extension Points Reference > org.eclipse.ui.actionSets**,** Eclipse online article Contributing Actions to the Eclipse Workbench

