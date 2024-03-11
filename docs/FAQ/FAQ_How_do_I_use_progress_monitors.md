

FAQ How do I use progress monitors?
===================================

A progress monitor is a callback interface that allows a long-running task to report progress and respond to cancellation. Typically, a UI component will create a monitor instance and pass it to a low-level component that does not know or care about the UI. Thus, an IProgressMonitor is an abstraction that allows for decoupling of UI and non-UI components.

Normally you won't use the IProgressMonitor interfaces. When you write a method that receives an IProgressMonitor, the first thing you will do is convert it to a SubMonitor via SubMonitor.convert.

This sets the number of units of work that it will take. The work value doesn't need to be very precise; your goal here is to give the user a rough estimate of how long it will take. If you have no way of estimating the amount of work, you can use the 1-argument version of SubMonitor.convert and then use the idiom subMonitor.setWorkRemaining(100).split(1) to allocate 1% of the remaining space on each iteration.

After allocating the units of work, you should call SubMonitor.split as the task progresses. The sum of the values passed to split method must equal the total work passed to SubMonitor.convert. Each call to SubMonitor.split returns a new SubMonitor which you can pass into another method that receives a progress monitor. If you just ignore the monitor returned by split, all of its progress will be reported the next time you use its parent monitor.

Here is a complete example of a long-running operation reporting progress:

      SubMonitor subMonitor = SubMonitor.convert(monitor, 10);
      subMonitor.setTaskName("Performing decathlon: ");
      subMonitor.subTask("hammer throw");
      //perform the hammer throw
      doHammerThrow(subMonitor.split(1));
      //... repeat for remaining nine events

The monitor can also be used to respond to cancellation requests. Each call to SubMonitor.split checks if the monitor has been cancelled and will throw OperationCanceledException if so.

[This article](https://eclipse.org/articles/Article-Progress-Monitors/article.html) offers some more useful examples.

See Also:
---------

*   The [Using Progress Monitors](https://eclipse.org/articles/Article-Progress-Monitors/article.html) article offers more useful examples
*   [FAQ How do I use a SubProgressMonitor?](./FAQ_How_do_I_use_a_SubProgressMonitor.md "FAQ How do I use a SubProgressMonitor?")
*   [FAQ Why should I use the new progress service?](./FAQ_Why_should_I_use_the_new_progress_service.md "FAQ Why should I use the new progress service?")

