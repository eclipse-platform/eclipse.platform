

FAQ How do I access the active project?
=======================================

This question is often asked by newcomers to Eclipse, probably as a result of switching from another IDE with a different interpretation of the term _project_. Similarly, people often ask how to access the active file. In Eclipse there is no such thing as an active project or file. Projects can be opened or closed, but many projects may be open at any given time.

Often people are really asking for the currently selected project, folder, or file. The selection can be queried using the UI's ISelectionService.

Once you have the selection, you can extract the selected resource as follows:

      IResource extractSelection(ISelection sel) {
         if (!(sel instanceof IStructuredSelection))
            return null;
         IStructuredSelection ss = (IStructuredSelection) sel;
         Object element = ss.getFirstElement();
         if (element instanceof IResource)
            return (IResource) element;
         if (!(element instanceof IAdaptable))
            return null;
         IAdaptable adaptable = (IAdaptable)element;
         Object adapter = adaptable.getAdapter(IResource.class);
         return (IResource) adapter;
      }

If you are looking for the active editor, you can determine that from the IPartService. If an editor is active, you can extract the resource, if available, like this:

      IResource extractResource(IEditorPart editor) {
         IEditorInput input = editor.getEditorInput();
         if (!(input instanceof IFileEditorInput))
            return null;
         return ((IFileEditorInput)input).getFile();
      }

The code above has a minor error:

      IEditorInput input = editor.getEditorInput();

To obtain the project from the resource use IResource.getProject(). Beware that while Eclipse uses "selected" rather than "active" for the active project, it uses "active" rather than "selected" for the active editor. Or is that "selected editor" ;-). For example,

      IWorkbench iworkbench = PlatformUI.getWorkbench();
      if (iworkbench == null)...
      IWorkbenchWindow iworkbenchwindow = iworkbench.getActiveWorkbenchWindow();
      if (iworkbenchwindow == null) ...
      IWorkbenchPage iworkbenchpage = iworkbenchwindow.getActivePage();
      if (iworkbenchpage == null) ...
      IEditorPart ieditorpart = iworkbenchpage.getActiveEditor();

See Also:
---------

*   [FAQ How do I find out what object is selected?](./FAQ_How_do_I_find_out_what_object_is_selected.md "FAQ How do I find out what object is selected?")
*   [Sample code to find selected project](http://dev.eclipse.org/mhonarc/lists/cdt-dev/msg11850.html)

