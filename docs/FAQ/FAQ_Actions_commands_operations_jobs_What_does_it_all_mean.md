

FAQ Actions, commands, operations, jobs: What does it all mean?
===============================================================

Many terms are thrown around in Eclipse for units of functionality or behavior: runnables, actions, commands, operations, and jobs, among others. Here is a high- level view of the terms to help you keep things straight.

Contents
--------

*   [1 Actions](#Actions)
*   [2 Commands](#Commands)
*   [3 Operations](#Operations)
*   [4 Jobs](#Jobs)
*   [5 See Also](#See-Also)

Actions
-------

As a plug-in writer, you will be interested mostly in _actions_. When a toolbar button or a menu item is clicked or when a defined key sequence is invoked, the run method of some action is invoked. Actions generally do not care about how or when they are invoked, although they often require extra context information, such as the current selection or view. Actions that are contributed to the workbench declaratively in plugin.xml defer the actual work to an _action delegate_.

_Note: When using action delegates, keep in mind that your implementing classes must provide a no argument constructor for RCP to hook the two together._

Commands
--------

_Commands_ are a meta-level glue that the platform uses to manage and organize actions. As a plug-in writer, you will never write code for a command directly, but you will use the org.eclipse.ui.commands extension point to define key bindings for your actions and to group your actions into configurations and contexts.

Operations
----------

_Operations_ aren't an official part of the workbench API, but the term tends to be used for a long-running unit of behavior. Any work that might take a second or more should really be inside an operation. The official designation for operations in the API is IRunnableWithProgress, but the term _operation_ tends to be used in its place because it is easier to say and remember. Operations are executed within an IRunnableContext. The context manages the execution of the operation in a non-UI thread so that the UI stays alive and painting. The context provides progress feedback to the user and support for cancellation.

Jobs
----

_Jobs_, introduced in Eclipse 3.0, are operations that run in the background. The user is typically prevented from doing anything while an operation is running but is free to continue working when a job is running. Operations and jobs belong together, but jobs needed to live at a lower level in the plug-in architecture to make them usable by non-UI components.

See Also
--------

*   [FAQ Does the platform have support for concurrency?](./FAQ_Does_the_platform_have_support_for_concurrency.md "FAQ Does the platform have support for concurrency?")
*   [FAQ What are IWorkspaceRunnable, IRunnableWithProgress, and WorkspaceModifyOperation?](./FAQ_What_are_IWorkspaceRunnable_IRunnableWithProgress_and_WorkspaceModifyOperation "FAQ What are IWorkspaceRunnable, IRunnableWithProgress, and WorkspaceModifyOperation?")

