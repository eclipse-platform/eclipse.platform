

FAQ How do I prevent builds between multiple changes to the workspace?
======================================================================

Every time resources in the workspace change, a resource change notification is broadcast, and autobuild gets a chance to run. This can become very costly if you are making several changes in succession to the workspace. To avoid these extra builds and notifications, it is very important that you batch all of your workspace changes into a single _workspace operation_. It is easy to accidentally cause extra builds if you aren't very careful about batching your changes. For example, even creating and modifying attributes on IMarker objects will cause separate resource change events if they are not batched.

Two different mechanisms are available for batching changes. To run a series of changes in the current thread, use IWorkspaceRunnable. Here is an example of a workspace runnable that creates two folders:

      final IFolder folder1 = ..., folder2 = ...;
      workspace.run(new IWorkspaceRunnable() {
         public void run(IProgressMonitor monitor) {
            folder1.create(IResource.NONE, true, null);
            folder2.create(IResource.NONE, true, null);
         }
      }, null);

The other mechanism for batching resource changes is a WorkspaceJob. Introduced in Eclipse 3.0, this mechanism is the asynchronous equivalent of IWorkspaceRunnable. When you create and schedule a workspace job, it will perform the changes in a background thread and then cause a single resource change notification and autobuild to occur. Here is sample code using a workspace job:

      final IFolder folder1 = ..., folder2 = ...;
      Job job = new WorkspaceJob("Creating folders") {
         public IStatus runInWorkspace(IProgressMonitor monitor) 
            throws CoreException {
            folder1.create(IResource.NONE, true, null);
            folder2.create(IResource.NONE, true, null);
            return Status.OK_STATUS;
         }
      };
      job.schedule();

See Also:
---------

*   [FAQ Does the platform have support for concurrency?](./FAQ_Does_the_platform_have_support_for_concurrency.md "FAQ Does the platform have support for concurrency?")
*   [FAQ What are IWorkspaceRunnable, IRunnableWithProgress, and WorkspaceModifyOperation?](./FAQ_What_are_IWorkspaceRunnable_IRunnableWithProgress_and_WorkspaceModifyOperation.msd "FAQ What are IWorkspaceRunnable, IRunnableWithProgress, and WorkspaceModifyOperation?")

