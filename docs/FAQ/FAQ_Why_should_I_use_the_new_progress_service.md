

FAQ Why should I use the new progress service?
==============================================

Eclipse 3.0 introduced a central new workbench _progress service_. This service combines the advantages of busy cursors and Progress dialogs by switching from one to the other, depending on the length of the long-running task. The service also handles a further wrinkle caused by the introduction of background jobs. It is now possible for a short-running task to become blocked by a longer-running job running in the background, owing to contention for various resources. When this happens, the progress service opens a richer Progress dialog with a details area showing all running background jobs. This allows the user to see what is happening and to cancel either the foreground or the background task, depending on which is more important.

The service is used much like the SWT BusyIndicator. Simply pass an IRunnableWithProgress instance to the busyCursorWhile method. The UI will prevent further user input and report progress feedback until the runnable completes. Note that the runnable executes in a non-UI thread, so you will have to use asyncExec or syncExec to execute any code within the runnable that requires access to UI widgets:

         IWorkbench wb = PlatformUI.getWorkbench();
         IProgressService ps = wb.getProgressService();
         ps.busyCursorWhile(new IRunnableWithProgress() {
            public void run(IProgressMonitor pm) {
               ... do some long running task
            }
         });

This progress service was introduced to unify a number of progress-reporting mechanisms in Eclipse 2.1. JFace provides a Progress Monitor dialog, SWT provides a busy indicator, and the workbench provides a progress indicator on the status line. Each of these mechanisms has its own advantages and disadvantages. The busy cursor is the least obtrusive and works well for tasks that typically take a second or less. The Progress dialog provides much more information and allows the user to cancel but is visually distracting, especially on short tasks as it pops up over the user's work. The status line progress monitor is a bit less obtrusive but doesn't give an obvious indication that the UI is not accepting further input, and the space for presenting progress indication is very constrained. The new progress service tries to achieve a balance by automatically adapting between a busy cursor and a dialog, depending on the situation.

See Also:
---------

*   [FAQ Why do I get an invalid thread access exception?](./FAQ_Why_do_I_get_an_invalid_thread_access_exception.md "FAQ Why do I get an invalid thread access exception?")
*   [FAQ How do I switch from using a Progress dialog to the Progress view?](./FAQ_How_do_I_switch_from_using_a_Progress_dialog_to_the_Progress_view.md "FAQ How do I switch from using a Progress dialog to the Progress view?")
*   [FAQ Actions, commands, operations, jobs: What does it all mean?](./FAQ_Actions_commands_operations_jobs_What_does_it_all_mean.md "FAQ Actions, commands, operations, jobs: What does it all mean?")
*   [FAQ What are IWorkspaceRunnable, IRunnableWithProgress, and WorkspaceModifyOperation?](./FAQ_What_are_IWorkspaceRunnable_IRunnableWithProgress_and_WorkspaceModifyOperation "FAQ What are IWorkspaceRunnable, IRunnableWithProgress, and WorkspaceModifyOperation?")

