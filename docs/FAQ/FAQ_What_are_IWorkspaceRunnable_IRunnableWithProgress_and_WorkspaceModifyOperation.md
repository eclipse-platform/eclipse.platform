

FAQ What are IWorkspaceRunnable, IRunnableWithProgress, and WorkspaceModifyOperation?
=====================================================================================

IWorkspaceRunnable is a mechanism for batching a set of changes to the workspace so that change notification and autobuild are deferred until the entire batch completes. IRunnableWithProgress is a mechanism for batching a set of changes to be run outside the UI thread. You often need to do both of these at once: Make multiple changes to the workspace outside the UI thread. Wrapping one of these mechanisms inside the other would do the trick, but the resulting code is cumbersome, and it is awkward to communicate arguments, results, and exceptions between the caller and the operation to be run.

The solution is to use WorkspaceModifyOperation. This class rolls the two mechanisms together by implementing IRunnableWithProgress and performing the work within a nested IWorkspaceRunnable. To use it, simply create a subclass that implements the abstract method execute, and pass an instance of this subclass to IRunnableContext.run to perform the work. If you already have an instance of IRunnableWithProgress on hand, it can be passed to the constructor of the special subclass WorkspaceModifyDelegatingOperation to create a new IRunnableWithProgress that performs workspace batching for you.

See Also:
---------

*   [FAQ Actions, commands, operations, jobs: What does it all mean?](./FAQ_Actions_commands_operations_jobs_What_does_it_all_mean.md "FAQ Actions, commands, operations, jobs: What does it all mean?")
*   [FAQ How do I prevent builds between multiple changes to the workspace?](./FAQ_How_do_I_prevent_builds_between_multiple_changes_to_the_workspace.md "FAQ How do I prevent builds between multiple changes to the workspace?")

