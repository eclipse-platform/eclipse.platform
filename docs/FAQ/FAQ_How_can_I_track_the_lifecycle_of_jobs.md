

FAQ How can I track the lifecycle of jobs?
==========================================

It is quite simple to find out when jobs, including those owned by others, are scheduled, run, awoken, and finished. As with many other facilities in the Eclipse Platform, a simple listener suffices:

    IJobManager manager = Job.getJobManager()
    manager.addJobChangeListener(new JobChangeAdapter() {
            public void scheduled(IJobChangeEvent event) {
                Job job = event.getJob();
                System.out.println("Job scheduled: " + job.getName());
            }
    });

By subclassing JobChangeAdapter, rather than directly implementing IJobChangeListener, you can pick and choose which job change events you want to listen to. Note that the done event is sent regardless of whether the job was cancelled or failed to complete, and the result status in the job change event will tell you how it ended.

See Also:
---------

*   [FAQ Does the platform have support for concurrency?](./FAQ_Does_the_platform_have_support_for_concurrency.md "FAQ Does the platform have support for concurrency?")
*   [FAQ How do I prevent two jobs from running at the same time?](./FAQ_How_do_I_prevent_two_jobs_from_running_at_the_same_time.md "FAQ How do I prevent two jobs from running at the same time?")

