

FAQ How do I open an editor on a file in the workspace?
=======================================================

In Eclipse 2.1, use the openEditor methods on IWorkbenchPage to open files in the workspace. In 3.0, this API was moved to the IDE class in order to remove the dependency between the generic workbench and the workspace. If you use openEditor(IFile), the platform will guess the appropriate editor to use, based on the file extension.

  
To open an editor to a particular position, you can create a marker in the file and then use openEditor(IMarker). Be sure to get rid of the marker when you're done. You can specify what editor to open by setting the EDITOR_ID_ATTR on the marker. If you don't do this, the workbench will guess what kind of editor to open from the file extension. The following code snippet opens the default text editor to line 5, using a marker:

      IFile file = <choose the file to open>;
      IWorkbenchPage page = <the page to open the editor in>;
      HashMap map = new HashMap();
      map.put(IMarker.LINE_NUMBER, new Integer(5));
      map.put(IWorkbenchPage.EDITOR\_ID\_ATTR, 
         "org.eclipse.ui.DefaultTextEditor");
      IMarker marker = file.createMarker(IMarker.TEXT);
      marker.setAttributes(map);
      //page.openEditor(marker); //2.1 API
      IDE.openEditor(marker); //3.0 API
      marker.delete();

See Also:
---------

*   [FAQ How do I open an editor programmatically?](./FAQ_How_do_I_open_an_editor_programmatically.md "FAQ How do I open an editor programmatically?")
*   [FAQ How do I open an editor on a file outside the workspace?](./FAQ_How_do_I_open_an_editor_on_a_file_outside_the_workspace.md "FAQ How do I open an editor on a file outside the workspace?")
*   [FAQ How do I open an editor on something that is not a file?](./FAQ_How_do_I_open_an_editor_on_something_that_is_not_a_file.md "FAQ How do I open an editor on something that is not a file?")

