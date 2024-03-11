

FAQ How do I open an editor programmatically?
=============================================

Use the openEditor methods on org.eclipse.ui.IWorkbenchPage to open an editor on a given input. The openEditor methods require you to supply the ID of the editor to open. You can use the editor registry to find out what editor ID is appropriate for a given file name, using the getDefaultEditor method on IEditorRegistry. In Eclipse 3.0, the editor opening methods that were specific to IFile were moved to the IDE class.

        IWorkbenchPage page = ...;
        IFile file = ...;
        IEditorDescriptor desc = PlatformUI.getWorkbench().
                getEditorRegistry().getDefaultEditor(file.getName());
        page.openEditor(new FileEditorInput(file), desc.getId());

This code needs to run on the UI thread and result from getActiveWorkbenchWindow() and getActivePage() need to be checked against null.

Opening External Files in Eclipse 3.3
-------------------------------------

The code below is for opening files that are not in the workspace (and hence are not IFiles) in Eclipse 3.3 and higher using [EFS](/EFS "EFS").

        import java.io.File;
        import org.eclipse.core.filesystem.EFS;
        import org.eclipse.core.filesystem.IFileStore;
        import org.eclipse.ui.PartInitException;
        import org.eclipse.ui.IWorkbenchPage;
        import org.eclipse.ui.PlatformUI;
        import org.eclipse.ui.ide.IDE;
         
        File fileToOpen = new File("externalfile.xml");
         
        if (fileToOpen.exists() && fileToOpen.isFile()) {
            IFileStore fileStore = EFS.getLocalFileSystem().getStore(fileToOpen.toURI());
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
         
            try {
                IDE.openEditorOnFileStore( page, fileStore );
            } catch ( PartInitException e ) {
                //Put your exception handler here if you wish to
            }
        } else {
            //Do something if the file does not exist
        }

See Also:
---------

*   [FAQ Is Eclipse 3.0 going to break all of my old plug-ins?](./FAQ_Is_Eclipse_3.0_going_to_break_all_of_my_old_plug-ins.md "FAQ Is Eclipse 3.0 going to break all of my old plug-ins?")
*   [FAQ How do I find the active workbench page?](./FAQ_How_do_I_find_the_active_workbench_page.md "FAQ How do I find the active workbench page?")
*   [FAQ How do I open an editor on a file in the workspace?](./FAQ_How_do_I_open_an_editor_on_a_file_in_the_workspace.md "FAQ How do I open an editor on a file in the workspace?")
*   [FAQ Can I make a job run in the UI thread?](./FAQ_Can_I_make_a_job_run_in_the_UI_thread.md "FAQ Can I make a job run in the UI thread?")

