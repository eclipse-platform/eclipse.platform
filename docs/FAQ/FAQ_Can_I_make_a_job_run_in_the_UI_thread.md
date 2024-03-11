

FAQ Can I make a job run in the UI thread?
==========================================

If you create and schedule a simple job, its run method will be called outside the UI thread. If you want to schedule a job that accesses UI widgets, you should subclass org.eclipse.ui.progress.UIJob instead of the base Job class. The UI job's runInUIThread method will, as the name implies, always be invoked from the UI thread. Make sure that you don't schedule UI jobs that take a long time to run because anything that runs in the UI thread will make the UI unresponsive until it completes.

Although running a UI job is much like using the SWT's Display methods asyncExec and timerExec, UI jobs have a few distinct advantages.

*   They can have scheduling rules to prevent them from running concurrently with jobs in other threads that may conflict with them.
*   You can install a listener on the job to find out when it completes.
*   You can specify a priority on the job so the platform can decide not to run it immediately if it is not as important.

See Also:
---------

*   [FAQ Does the platform have support for concurrency?](./FAQ_Does_the_platform_have_support_for_concurrency.md "FAQ Does the platform have support for concurrency?")
*   [FAQ Why do I get an invalid thread access exception?](./FAQ_Why_do_I_get_an_invalid_thread_access_exception.md "FAQ Why do I get an invalid thread access exception?")

