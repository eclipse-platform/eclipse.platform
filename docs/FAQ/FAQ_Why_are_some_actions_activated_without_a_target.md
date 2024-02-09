

FAQ Why are some actions activated without a target?
====================================================

Most of the time, an action is invoked on the _current selection_ in the UI. One such example is the **Properties** menu action on a resource in the Navigator. This action is relevant only when a particular object has been selected. In other cases, the action is _global_, as is the action behind **Project > Build All**, which simply builds all projects in the workspace and is independent of the current selection.

The workbench does not distinguish between context-sensitive actions and global actions, and so it calls the selectionChanged method on all action delegates in case they want to update their enablement based on the selection. This is somewhat of a design mistake as many unnecessary selection change notifications are now sent out throughout the workbench. Furthermore, actions have to remember the current selection and store it in a field so it can be used if the run method is called. This is not a good design pattern.

Luckily, the workbench added an interface called IActionDelegate2 in version 2.0 of the platform. When clients implement both IActionDelegate and IActionDelegate2, the workbench will not call the run(IAction action) method but will call the runWithEvent method instead.

This alternative solution allows clients to write more compact code with control flow localized to the execution of the action. A typical action would declare itself as follows:

      class Handler extends ActionDelegate 
                                 implements IActionDelegate2 {
         public void runWithEvent(IAction action, Event event) {
            MessageDialog.openInformation(new Shell(), 
               "Demo", "Handling: "+action+" on "+event);
         }
      }

Interfaces like IActionDelegate2 are an indication of how a platform like Eclipse struggles with the tension between adoption and innovation. Because too many client plug-ins already depended on the implementation of the old interface IActionDelegate, it could not be easily changed without breaking the existing Eclipse API. Instead, a parallel replacement was added, which is less elegant but comes back in multiple places in Eclipse. The existence of the IActionDelegateWithEvent interface shows how even naming mistakes have to persist for a while as some clients may rely on it.

