

FAQ Does the platform have support for concurrency?
===================================================

In Eclipse 3.0, infrastructure was added to support running application code concurrently. This support is useful when your plug-in has to perform some CPU-intensive work and you want to allow the user to continue working while it goes on. For example, a Web browser or mail client typically fetches content from a server in the background, allowing the user to browse existing content while waiting.

The basic unit of concurrent activity in Eclipse is provided by the Job class. Jobs are a cross between the interface java.lang.Runnable and the class java.lang.Thread. Jobs are similar to runnables because they encapsulate behavior inside a run method and are similar to threads because they are not executed in the calling thread. This example illustrates how a job is used:

   Job myJob = new Job("Sample Job") {
      public IStatus run(IProgressMonitor monitor) {
         System.out.println("This is running in a job");
         return Status.OK_STATUS;
      }
   };
   myJob.schedule();

When schedule is called, the job is added to a queue of jobs waiting to be run. Worker threads then remove them from the queue and invoke their run method. This system has a number of advantages over creating and starting a Java thread:

*   _Less overhead_. Creating a new thread every time you want to run something can be expensive. The job infrastructure uses a thread pool that reuses the same threads for many jobs.

*   _Support for progress and cancellation_. Jobs are provided with a progress monitor object, allowing them to respond to cancellation requests and to report how much work they have done. The UI can listen to these progress messages and display feedback as the job executes.

*   _Support for priorities and mutual exclusion_. Jobs can be configured with varying priorities and with scheduling rules that describe when jobs can be run concurrently with other jobs.

*   _Advanced scheduling features_. You can schedule a job to run at any time in the future and to reschedule itself after completing.

Note that the same job instance can be rerun as many times as you like, but you cannot schedule a job that is already sleeping or waiting to run. Jobs are often written as singletons, both to avoid the possibility of the same job being scheduled multiple times and to avoid the overhead of creating a new object every time it is run.

See Also:
---------

*   [FAQ How do I show progress for things happening in the background?](./FAQ_How_do_I_show_progress_for_things_happening_in_the_background.md "FAQ How do I show progress for things happening in the background?")
*   [FAQ How do I switch from using a Progress dialog to the Progress view?](./FAQ_How_do_I_switch_from_using_a_Progress_dialog_to_the_Progress_view.md "FAQ How do I switch from using a Progress dialog to the Progress view?")
*   [FAQ Actions, commands, operations, jobs: What does it all mean?](./FAQ_Actions_commands_operations_jobs_What_does_it_all_mean.md "FAQ Actions, commands, operations, jobs: What does it all mean?")
*   [On the Job: The Eclipse Jobs API](https://www.eclipse.org/articles/Article-Concurrency/jobs-api.html)
*   [Eclipse Jobs and Background processing Tutorial](https://www.vogella.com/tutorials/EclipseJobs/article.html)

