

FAQ How do I prevent two jobs from running at the same time?
============================================================

The platform job mechanism uses a pool of threads, allowing it to run several jobs at the same time. If you have many jobs that you want to run in the background, you may want to prevent more than one from running at once. For example, if the jobs are accessing an exclusive resource, such as a file or a socket, you won't want them to run simultaneously. This is accomplished by using job-scheduling rules. A scheduling rule contains logic for determining whether it conflicts with another rule. If two rules conflict, two jobs using those rules will not be run at the same time. The following scheduling rule will act as a mutex, not allowing two jobs with the same rule instance to run concurrently:

      public class MutexRule implements ISchedulingRule {
         public boolean isConflicting(ISchedulingRule rule) {
            return rule == this;
         }
         public boolean contains(ISchedulingRule rule) {
            return rule == this;
         }
      }

The rule is then used as follows:

      Job job1 = new SampleJob();
      Job job2 = new SampleJob();
      MutexRule rule = new MutexRule();
      job1.setRule(rule);
      job2.setRule(rule);
      job1.schedule();
      job2.schedule();

When this example is executed, job1 will start running immediately, and job2 will be blocked until job1 finishes. Once job1 is finished, job2 will be run automatically.

You can create your own scheduling rules, which means that you have complete control of the logic for the isConflicting relation. For example, the resources plug-in has scheduling rules for files such that two files conflict if one is a parent of the other. This allows a job to have exclusive access to a complete file-system subtree.

The contains relation on scheduling rules is used for a more advanced feature of scheduling rules. Multiple rules can be owned by a thread at a given time only if the subsequent rules are all contained within the initial rule. For example, in a file system, a thread that owns the scheduling rule for c:\\a\\b can acquire a rule for the subdirectory c:\\a\\b\\c. A thread acquires multiple rules by using the IJobManager methods beginRule and endRule. Use extreme caution when using these methods; if you begin a rule without ending it, you will lock that rule forever.

See Also:
---------

*   [FAQ Does the platform have support for concurrency?](./FAQ_Does_the_platform_have_support_for_concurrency.md "FAQ Does the platform have support for concurrency?")

