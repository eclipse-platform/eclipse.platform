

FAQ How do I use a SubProgressMonitor?
======================================

You shouldn't use SubProgressMonitor. You should use SubMonitor. More information about migrating SubProgressMonitor to SubMonitor can be found [here](https://eclipse.org/articles/Article-Progress-Monitors/article.html)

When using progress monitors in Eclipse, an important rule is that all API methods expect a fresh, unused progress monitor. You cannot pass them a monitor that has had beginTask called on it or a monitor that has already recorded some units of work. The reasons for this are clear. API methods can be called from a variety of places and cannot predict how many units of work they represent in the context of a long-running operation. An API method that deletes a file might represent all the work for an operation or might be called as a small part of a much larger operation. Only the code at the top of the call chain has any way of guessing how long the file deletion will take in proportion to the rest of the task.

But if every API method you call expects a fresh monitor, how do you implement an operation that calls several such API methods? The solution is to use a SubMonitor, which acts as a bridge between caller and callee. This monitor knows how many work units the parent has allocated for a given work task and how many units of work the child task thinks it has. When the child task reports a unit of progress, the SubMonitor scales that work in proportion to the number of parent work units available.

If you are lost at this point, it is probably best to look at a simple example. This fictional move method is implemented by calling a copy method, followed by a delete method. The move method estimates that the copying will take 80 percent of the total time, and that the deletion will take 20 percent of the time:

      public void move(File a, File b, IProgressMonitor monitor) {
         SubMonitor subMonitor = SubMonitor.convert(monitor, 10);
         copy(a, b, subMonitor.split(8));
         delete(a, subMonitor.split(2));
      }

The copy and delete methods, in turn, will call beginTask on the SuMonitor that was allocated to it. The copy method might decide to report one unit of work for each 8KB chunk of the file. Regardless of the size of the file, the SubMonitor knows that it can report only eight units of work to the parent monitor and so it will scale reported work units accordingly.

See Also:
---------

*   [FAQ How do I use progress monitors?](./FAQ_How_do_I_use_progress_monitors.md "FAQ How do I use progress monitors?")

