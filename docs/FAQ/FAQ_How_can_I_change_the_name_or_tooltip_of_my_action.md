

FAQ How can I change the name or tooltip of my action?
======================================================

Actions contributed via XML have statically defined names, images, and tooltips. Often, you want to change these attributes dynamically, based on the current selection or some other state. Owing to the lazy-loading nature of the platform, you can't do this until your action has been run once. A common solution is to specify generic attributes in the XML and then make attributes more dynamic after the action has been run. For example, the action to enable or disable breakpoints is called **Toggle Breakpoint** when the platform is first started. After it has been run once, the action dynamically sets its name to be either **Enable Breakpoint** or **Disable Breakpoint**, depending on whether the selected breakpoint is enabled. Here is an example action that implements this behavior:

      class ToggleAction 
            implements IWorkbenchWindowActionDelegate {
         private boolean state = false;
         public void run(IAction action) {
            state = !state;
            String name = state ? "True Action" : "False Action";
            action.setText(name);
            action.setToolTipText(name);
         }
      }

  
You can change many more action properties, including the action's image, accelerator key, and enablement. Look at the methods on IAction to see what other properties can be changed.

It is also common to update action properties when the selection changes. Although the preceding example uses a workbench window action delegate, which is not notified of selection changes by default, the action can register itself as a selection listener in the init method and then update its state during the selectionChanged callback.

  

See Also:
---------

[FAQ\_How\_do\_I\_find\_out\_what\_object\_is_selected?](./FAQ_How_do_I_find_out_what_object_is_selected.md "FAQ How do I find out what object is selected?")

