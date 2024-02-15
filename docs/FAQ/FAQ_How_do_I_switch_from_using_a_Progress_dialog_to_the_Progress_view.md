

FAQ How do I switch from using a Progress dialog to the Progress view?
======================================================================

If you have an existing plug-in that uses a ProgressMonitorDialog, you can easily switch to using the Progress view by rewriting your operation as a org.eclipse.core.runtime.Job. Assume that your original code looks like this:

      IRunnableWithProgress op = new IRunnableWithProgress() {
         public void run(IProgressMonitor monitor) {
            runDecathlon(monitor);
         }
      };
      IWorkbench wb = PlatformUI.getWorkbench();
      IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
      Shell shell = win != null ? win.getShell() : null;
      new ProgressMonitorDialog(shell).run(true, true, op);

The equivalent code using org.eclipse.core.runtime.Job would look like this:

      class DecathlonJob extends Job {
         public DecathlonJob() {
            super("Athens decathlon 2004");
         }
         public IStatus run(IProgressMonitor monitor) {
            runDecathlon(monitor);
            return Status.OK_STATUS;
         }
      };
      new DecathlonJob().schedule();

Both use an IProgressMonitor to report progress to the user. The major difference is that the ProgressMonitorDialog is a modal dialog and blocks access to the entire UI during the execution of runDecathlon. When a Job is used, it will run in the background, and the user can continue working on something else.

Although the changes required here appear to be simply cosmetic, keep in mind that there are subtle implications to running your operation in the background. Foremost, you must ensure that your operation code is thread safe in case two copies of the operation start running simultaneously. You also need to think about whether your background operation will be in contention with other ongoing processes for exclusive resources, such as Java object monitors. Contention between threads can block the user interface; worse, it can lead to deadlock. Read up on your concurrent programming before you venture down this path.

See Also:
---------

*   [FAQ How do I use progress monitors?](./FAQ_How_do_I_use_progress_monitors.md "FAQ How do I use progress monitors?")
*   [FAQ Does the platform have support for concurrency?](./FAQ_Does_the_platform_have_support_for_concurrency.md "FAQ Does the platform have support for concurrency?")
*   [FAQ Actions, commands, operations, jobs: What does it all mean?](./FAQ_Actions_commands_operations_jobs_What_does_it_all_mean.md "FAQ Actions, commands, operations, jobs: What does it all mean?")

