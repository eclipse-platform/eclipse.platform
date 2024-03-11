

FAQ How do I enable global actions such as Cut, Paste, and Print in my editor?
==============================================================================

Your editor's IEditorActionBarContributor, defined in the editor definition in the plugin.xml file, is responsible for enabling global actions. Whenever your editor becomes the active part, the method setActiveEditor is called on the action bar contributor. This is where you can retarget the global actions for your editor. Keep in mind that each editor type has only one editor action bar contributor, so you need to update your actions to reflect the current editor. In this example, the global **Print** action is being retargeted to the active editor:

      IAction print = ...;
      public void setActiveEditor(IEditorPart part) {
         IActionBars bars= getActionBars();
         if (bars == null)
            return;
         print.setEditor(part);
         bars.setGlobalActionHandler(
            IWorkbenchActionConstants.PRINT, print);
         bars.updateActionBars();
      }

  

See Also
--------

[FAQ How do I hook into global actions, such as Copy and Delete?](./FAQ_How_do_I_hook_into_global_actions_such_as_Copy_and_Delete.md "FAQ How do I hook into global actions, such as Copy and Delete?")

