

FAQ What is the purpose of job families?
========================================

Several methods on IJobManager (find, cancel, join, sleep, and wakeUp) require a job family object as parameter. A job family can be any object and simply acts as an identifier to group and locate job instances. Job families have no effect on how jobs are scheduled or executed. If you define your job to belong to a family, you can use it to distinguish among various groups or classifications of jobs for your own purposes.

A concrete example will help to explain how families can be used. The Java search mechanism uses background jobs to build indexes of source files and JARs in each project of the workspace. If a project is deleted, the search facility wants to discard all indexing jobs on files in that project as they are no longer needed. The search facility accomplishes this by using project names as a family identifier. A simplified version of the index job implementation is as follows:

      class IndexJob extends Job {
         String projectName;
         ...
         public boolean belongsTo(Object family) {
            return projectName.equals(family);
         }
      }

When a project is deleted, all index jobs on that project can be cancelled using the following code:

   IProject project = ...;
   Job.getJobManager().cancel(project.getName());

The belongsTo method is the place where a job specifies what families, if any, it is associated with. The advantage of placing this logic on the job instance itself-rather than having jobs publish a family identifier and delegate the matching logic to the job manager-is that it allows a job to specify that it belongs to several families. A job can even dynamically change what families it belongs to, based on internal state. If you have no use for job families, don't override the belongsTo method. By default, jobs will not belong to any families.

See Also:
---------

*   [FAQ Does the platform have support for concurrency?](./FAQ_Does_the_platform_have_support_for_concurrency.md "FAQ Does the platform have support for concurrency?")
*   [FAQ How do I prevent two jobs from running at the same time?](./FAQ_How_do_I_prevent_two_jobs_from_running_at_the_same_time.md "FAQ How do I prevent two jobs from running at the same time?")

