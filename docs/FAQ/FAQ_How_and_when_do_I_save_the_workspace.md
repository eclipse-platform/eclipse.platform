

FAQ How and when do I save the workspace?
=========================================

_Please note that this state information is **not** related to the list of currently opened editors, views, and perspectives in the workbench windows._

If you're using the Eclipse IDE workbench, you don't. When it shuts down, the workbench will automatically save the workspace. The workspace will also perform its own periodic workspace saves, called _snapshots_, every once in a while. Note that the most essential information in the workspace-such as newly created files and folders within Eclipse-are always stored on disk immediately. Saving the workspace simply involves storing away metadata, such as markers, and its in-memory picture of the projects. The workspace is designed so that if a user pulls the computer power cord from the wall at any moment, the resource tree will still be in a good state so that the workspace will be able to restart in a consistent state with minimal loss of information.

Nonetheless, it is possible for your plug-in to explicitly request a workspace save or snapshot. If you are writing an RCP Application, you are responsible for minimally invoking save before shutdown.

The following example saves the workspace:

    final MultiStatus status = new MultiStatus(...);
    IRunnableWithProgress runnable = new IRunnableWithProgress() {
      public void run(IProgressMonitor monitor) {
        try {
          IWorkspace ws = ResourcesPlugin.getWorkspace();
          status.merge(ws.save(true, monitor));
        } catch (CoreException e) {
          status.merge(e.getStatus());
        }
      }
    };
    new ProgressMonitorDialog(null).run(false, false, runnable);
    if (!status.isOK()) {
      ErrorDialog.openError(...);
    }

Note that the save method can indicate minor problems by returning an IStatus object, or major problems by throwing an exception. You should check both of these results and react accordingly. To request a workspace snapshot, the code is almost identical: pass false as the first parameter to the save method.

See Also:
---------

[FAQ\_How\_do\_I\_make\_the\_workbench_shutdown?](./FAQ_How_do_I_make_the_workbench_shutdown.md "FAQ How do I make the workbench shutdown?")

[FAQ\_How\_can\_I\_be\_notified\_when\_the\_workspace\_is\_being_saved?](./FAQ_How_can_I_be_notified_when_the_workspace_is_being_saved.md "FAQ How can I be notified when the workspace is being saved?")

[Bug 337593](https://bugs.eclipse.org/bugs/show_bug.cgi?id=337593) has a plug-in attached which saves the workbench state after every change.

