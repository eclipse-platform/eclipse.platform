

FAQ How do I find out whether a particular job is running?
==========================================================

If you have a reference to a job instance, you can use the method Job.getState to find out whether it is running. Note that owing to the asynchronous nature of jobs, the result may be invalid by the time the method returns. For example, a job may be running at the time the method is called but may finish running between the time you invoke getState and the time you check its return value. For this reason, you should generally avoid relying too much on the result of this method.

The job infrastructure makes things easier for you by generally being very tolerant of methods called at the wrong time. For example, if you call wakeUp on a job that is not sleeping or cancel on a job that is already finished, the request is silently ignored. Thus, you can generally forgo the state check and simply try the method you want to call. For example, you do not need to do this:

        if (job.getState() == Job.NONE)
            job.schedule();

Instead, you can invoke job.schedule() immediately. If the job is already scheduled or sleeping, the schedule request will be ignored. If the job is currently running, it will be rescheduled as soon as it completes

If you need to be certain of when a job enters a particular state, register a job change listener on the job. Listeners are notified whenever jobs change state, so you can be sure that you will never miss a state change this way. Although the job may have changed state again by the time your listener is called, you are guaranteed that a given job listener will not receive multiple events concurrently or out of order.

If you do not have a job reference, you can search for it by using the method IJobManager.find. This method will find only job instances that are running, waiting, or sleeping. To give a concrete example, the Eclipse IDE uses this method when the user launches an application. The method searches for the autobuild job and, if it is running, waits for autobuild to complete before launching the application. Here is a snippet that illustrates this behavior; the actual code is more complex because it first consults a preference setting and might decide to prompt the user:

        IJobManager jobMan = Job.getJobManager();
        Job[] build = jobMan.find(ResourcesPlugin.FAMILY_AUTO_BUILD); 
        if (build.length == 1)
            build[0].join();

Again, it is safe to call join here without checking whether the job is still running. The join method will return immediately in this case.

See Also:
---------

*   [FAQ Does the platform have support for concurrency?](./FAQ_Does_the_platform_have_support_for_concurrency.md "FAQ Does the platform have support for concurrency?")
*   [FAQ What is the purpose of job families?](./FAQ_What_is_the_purpose_of_job_families.md "FAQ What is the purpose of job families?")
*   [FAQ How can I track the lifecycle of jobs?](./FAQ_How_can_I_track_the_lifecycle_of_jobs.md "FAQ How can I track the lifecycle of jobs?")

