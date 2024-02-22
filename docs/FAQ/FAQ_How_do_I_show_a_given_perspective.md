

FAQ How do I show a given perspective?
======================================

Two APIs show a perspective: IWorkbench.showPerspective and IWorkbenchPage.setPerspective. You should generally use the first of these two because it will honor the user's preference on whether perspectives should be opened in the same window or in a separate window. Here's a quick sample snippet that opens the perspective with ID perspectiveID:

      IWorkbench workbench = PlatformUI.getWorkbench();
      workbench.showPerspective(perspectiveID, 
         workbench.getActiveWorkbenchWindow());

Usually, the method getActiveWorkbenchWindow comes with the caveat that you must check for a null return value. In this case, null is acceptable: The showPerspective method uses the window parameter as a hint about what window to show the perspective in. If a null window is passed, it will pick an appropriate existing window or open a new one.

