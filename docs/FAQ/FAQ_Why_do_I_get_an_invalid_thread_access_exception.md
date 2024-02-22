

FAQ Why do I get an invalid thread access exception?
====================================================

On most operating systems, drawing to the screen is an operation that needs to be synchronized with other draw requests to prevent chaos. A simple OS solution for this resource-contention problem is to allow drawing operations to occur only in a special thread. Rather than drawing at will, an application sends in a request to the OS for a redraw, and the OS will, at a time it deems appropriate, call back the application. An SWT application behaves in the same way.

When the end user of your application activates a menu or clicks a button, the OS will notify SWT, which in turn will call anyone listening to that button, until eventually the call chain ends with you. All these calls are made in the same event loop thread. Normally, a UI does not act on its own but reacts to stimuli from others. In general, GUI applications are passive.

Therefore, when an application decides that it needs to live a life on its own, one option is for it to create another Java thread. A typical sample is the following:

      new Thread(new Runnable() {
         public void run() {
            while (true) {
               try { Thread.sleep(1000); } catch (Exception e) { }
               Display.getDefault().asyncExec(new Runnable() {
                  public void run() {
                     ... do any work that updates the screen ...
                  }
               });
            }
         }
      }).start();

This starts a timer that goes off every second and does some work. Because the work will be done in an unsafe thread, we need to request that SWT performs the task in a safe manner. We do this by requesting that the default SWT display runs our runnable when it can using asyncExec. In practice, this request is served as soon as possible. Execution is performed asynchronously. In other words, the request is placed in a queue with all other asyncExec requests and dealt with in a first-come first-served manner.

The call to Display.asyncExec returns immediately, before the drawing takes place. If you need to be guaranteed that the changes to the display took place before continuing, use Display.syncExec, which will suspend execution of the calling thread until the operation has finished.

Just about any SWT method that accesses or changes a widget must be called in the UI thread. If you are unsure, check the method javadoc. Any method that must be called in the UI thread will declare that it throws SWTException with value ERROR\_THREAD\_INVALID_ACCESS.

Avoid long-running processes in the UI thread as they will make the UI unresponsive. Do work that does not require UI access in a separate thread, and use the asyncExec call only for UI updates.

See Also:
---------

*   [FAQ Does the platform have support for concurrency?](./FAQ_Does_the_platform_have_support_for_concurrency.md "FAQ Does the platform have support for concurrency?")
*   [FAQ Can I make a job run in the UI thread?](./FAQ_Can_I_make_a_job_run_in_the_UI_thread.md "FAQ Can I make a job run in the UI thread?")

