

FAQ How can I be notified of changes to the workspace?
======================================================

Resource change listeners are notified of most changes that occur in the workspace, including when any file, folder, or project is created, deleted, or modified. Listeners can also be registered for some special events, such as before projects are deleted or closed and before and after workspace autobuilds. Registering a resource change listener is easy:

      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      IResourceChangeListener rcl = new IResourceChangeListener() {
         public void resourceChanged(IResourceChangeEvent event) {
         }
      };
      workspace.addResourceChangeListener(rcl);

Always make sure that you remove your resource change listener when you no longer need it:

      workspace.removeResourceChangeListener(rcl);

Look at the javadoc for IWorkspace.addResourceChangeListener for more information on the various types of resource change events you can listen to and the restrictions that apply. It is important to keep performance in mind when writing a resource change listener. Listeners are notified at the end of every operation that changes the workspace, so any overhead that you add in your listener will degrade the performance of all such operations. If your listener needs to do expensive processing, consider off-loading some of the work into another thread, preferably by using a Job as described in [FAQ Does the platform have support for concurrency?](./FAQ_Does_the_platform_have_support_for_concurrency.md "FAQ Does the platform have support for concurrency?")

See Also:
---------

*   [FAQ Does the platform have support for concurrency?](./FAQ_Does_the_platform_have_support_for_concurrency.md "FAQ Does the platform have support for concurrency?")
*   [Eclipse online article How You've Changed! Responding to resource changes in the Eclipse workspace](https://www.eclipse.org/articles/Article-Resource-deltas/resource-deltas.html)

