

FAQ How do I show progress for things happening in the background?
==================================================================

A Progress view was introduced in Eclipse 3.0 to provide feedback for activities occurring in the background. This view also allows the user to cancel background activity and to find out details when errors occur in the background. The progress animation icon on the right-hand side of the status line is also associated with this view. The icon is animated whenever anything is running in the background. There is no API for reporting progress directly to any of these progress indicators.

These indicators are used to show progress reported by Job objects. The only way to report progress in these areas is to create and schedule a background job. When a Job instance is executed, an IProgressMonitor instance is passed to the run method. Any progress sent to this monitor, including task and subtask names and units of work completed, will be shown in the Progress view. Use this monitor just as you would use a monitor in a Progress dialog.

See Also:
---------

*   [FAQ How do I use progress monitors?](./FAQ_How_do_I_use_progress_monitors.md "FAQ How do I use progress monitors?")
*   [FAQ Does the platform have support for concurrency?](./FAQ_Does_the_platform_have_support_for_concurrency.md "FAQ Does the platform have support for concurrency?")

