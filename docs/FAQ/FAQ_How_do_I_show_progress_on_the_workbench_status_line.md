

FAQ How do I show progress on the workbench status line?
========================================================

The status line has two areas for showing progress. The status line manager has a progress monitor that can be used when you want to block the user from continuing to work during an operation. This progress bar is used as follows:

      IActionBars bars = getViewSite().getActionBars();
      IStatusLineManager statusLine = bars.getStatusLineManager();
      IProgressMonitor pm = statusLine.getProgressMonitor();
      pm.beginTask("Doing work", IProgressMonitor.UNKNOWN);
      pm.worked(1);
      .... the actual work is done here...
      pm.done();

If the amount of work to be done can be estimated ahead of time, a more intelligent value can be passed to beginTask, and calls to worked can be used to provide better progress feedback than a continuous animation.

The far right-hand side of the status line is used to show progress for things happening in the background. In other words, when progress is shown here, the user can generally continue working while the operation runs.

See Also:
---------

*   [FAQ How do I use progress monitors?](./FAQ_How_do_I_use_progress_monitors.md "FAQ How do I use progress monitors?")
*   [FAQ Why should I use the new progress service?](./FAQ_Why_should_I_use_the_new_progress_service.md "FAQ Why should I use the new progress service?")
*   [FAQ How do I show progress for things happening in the background?](./FAQ_How_do_I_show_progress_for_things_happening_in_the_background.md "FAQ How do I show progress for things happening in the background?")

