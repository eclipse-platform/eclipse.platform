

FAQ How do I create a repeating background task?
================================================

It is common to have background work that repeats after a certain interval. For example, an optional background job refreshes the workspace with repository contents. The workspace itself saves a snapshot of its state on disk every few minutes, using a background job. Setting up a repeating job is not much more difficult than setting up a simple job. The following job reschedules itself to run once every minute:

      public class RepeatingJob extends Job {
         private boolean running = true;
         public RepeatingJob() {
            super("Repeating Job");
         }
         protected IStatus run(IProgressMonitor monitor) {
            schedule(60000);
            return Status.OK_STATUS;
         }
         public boolean shouldSchedule() {
            return running;
         }
         public void stop() {
            running = false;
         }
      }

      /*

      Sample  Code by Parvez Hakim
      www.abobjects.com

      */

      RepeatingJob job = new RepeatingJob("BackupScheduler"+jobNumber++,nextDelay) {			
               protected IStatus run(IProgressMonitor monitor){ 

                  schedule(repeatDelay-cpuTimeTakenbyJob);
                  return org.eclipse.core.runtime.Status.OK_STATUS;
               }
            };
            job.schedule(delay); // start after 20 seconds  
            return job;
      }

      public abstract class RepeatingJob extends Job{
         private boolean running = true;
         protected long repeatDelay = 0; 
         public RepeatingJob(String jobName,long repeatPeriod){ 
            super(jobName);
            repeatDelay = repeatPeriod;
         }/**
         protected IStatus run(IProgressMonitor monitor) {
            schedule(repeatDelay);
            return Status.OK_STATUS;
         }
         */ 
         public boolean shouldSchedule() {
            return running;
         }
         public void stop() {
            running = false;
         }
      }

  
The same schedule method that is used to get the job running in the first place is also used to reschedule the job while it is running. Calling schedule while a job is running will flag the job to be scheduled again as soon as the run method exits. It does not mean that the same job instance can be running in two threads at the same time, as the job is added back to the waiting queue only after it finishes running. Repeating jobs always need some rescheduling condition to prevent them from running forever. In this example, a simple flag is used to check if the job needs to be rescheduled. Before adding a job to the waiting queue, the framework calls the shouldSchedule method on the job. This allows a job to indicate whether it should be added to the waiting job queue. If the call to shouldSchedule returns false, the job is discarded. This makes it a convenient place for determining whether a repeating job should continue.

