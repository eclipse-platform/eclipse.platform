

FAQ How do I run a lengthy process in a wizard?
===============================================

The IWizardContainer passed to your wizard extends the interface called IRunnableContext. This means that you can pass it an IRunnableWithProgress and that it will give progress feedback to the user while it runs. Keep in mind that as with all long-running operations, the container will generally fork a different thread to run your operation. If you want to manipulate any widgets from the operation, you'll have to use Display.asyncExec. The FAQ Examples plug-in includes a sample wizard, called AddingWizard, that computes the sum of two integers, using the wizard container's progress monitor:

      getContainer().run(true, true, new IRunnableWithProgress() {
         public void run(IProgressMonitor monitor) {
            int sum = n1 + n2;
            monitor.beginTask("Computing sum: ", sum);
            for (int i = 0; i < sum; i++) {
               monitor.subTask(Integer.toString(i));
               //sleep to simulate long running operation
               Thread.sleep(100);
               monitor.worked(1);
            }
            monitor.done();
         }
      });

Your wizard can specify whether it needs a progress bar or a simple busy cursor. For operations that may take more than a second, you should use a progress bar. This is done by implementing needsProgressMonitor method on IWizard to return true.

See Also:
---------

*   [FAQ How do I use progress monitors?](./FAQ_How_do_I_use_progress_monitors.md "FAQ How do I use progress monitors?")
*   [FAQ Why do I get an invalid thread access exception?](./FAQ_Why_do_I_get_an_invalid_thread_access_exception.md "FAQ Why do I get an invalid thread access exception?")

