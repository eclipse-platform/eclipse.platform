

FAQ How do I open an editor on a file outside the workspace?
============================================================

Since 3.3 you can use the new EFS support to open an text editor on a file outside the workspace:

        String name = new FileDialog(aShell, SWT.OPEN).open();
        if (name == null)
            return;
        IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(filterPath));
        fileStore = fileStore.getChild(names[i]);
        if (!fileStore.fetchInfo().isDirectory() && fileStore.fetchInfo().exists()) {
            IWorkbenchPage page=  window.getActivePage();
            try {
                IDE.openEditorOnFileStore(page, fileStore);
            } catch (PartInitException e) {
                /* some code */
            }
        }

Alternatively, you can create a _linked resource_ in an existing project, which points to a file elsewhere in the file system. This example snippet creates a project called External Files, and then prompts the user to select any file in the file system. The code then creates a linked resource in the project to that external file, allowing the platform to open the file in read/write mode in one of the standard editors:

        IWorkspace ws = ResourcesPlugin.getWorkspace();
        IProject project = ws.getRoot().getProject("External Files");
        if (!project.exists())
            project.create(null);
        if (!project.isOpen())
            project.open(null);
        Shell shell = window.getShell();
        String name = new FileDialog(shell, SWT.OPEN).open();
        if (name == null)
            return;
        IPath location = new Path(name);
        IFile file = project.getFile(location.lastSegment());
        file.createLink(location, IResource.NONE, null);
        IWorkbenchPage page = window.getActivePage();
        if (page != null)
            page.openEditor(file);

See Also:
---------

*   [FAQ How do I accommodate project layouts that don't fit the Eclipse model?](./FAQ_How_do_I_accommodate_project_layouts_that_dont_fit_the_Eclipse_model.md "FAQ How do I accommodate project layouts that don't fit the Eclipse model?")
*   [FAQ How do I open an editor programmatically?](./FAQ_How_do_I_open_an_editor_programmatically.md "FAQ How do I open an editor programmatically?")
*   [FAQ How do I open an editor on something that is not a file?](./FAQ_How_do_I_open_an_editor_on_something_that_is_not_a_file.md "FAQ How do I open an editor on something that is not a file?")

