

FAQ How do I open an external editor?
=====================================

A special editor ID is used to indicate that a file should be opened using an external editor. When you ask it to open an editor with this ID, the platform delegates to the operating system to select and open an appropriate editor for the given input. This ID can be used to open an editor on IFile instances or on any other kind of input that implements IPathEditorInput. Here is an example snippet that opens an external editor on an IFile instance:

      IWorkbenchPage page = ...;
      IFile file = ...;
      page.openEditor(
         new FileEditorInput(file),
         IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);

Note that this technique applies only to Eclipse 3.0 or greater. On older versions of Eclipse, a special convenience method, openSystemEditor on IWorkbenchPage, accomplished the same task. This method was removed from the workbench API as part of the Eclipse 3.0 rich client refactoring.

See Also:
---------

*   [FAQ\_How\_do\_I\_find\_the\_active\_workbench\_page?](./FAQ_How_do_I_find_the_active_workbench_page.md "FAQ How do I find the active workbench page?")
*   [FAQ\_How\_do\_I\_open\_an\_editor\_on\_a\_file\_in\_the\_workspace?](./FAQ_How_do_I_open_an_editor_on_a_file_in_the_workspace.md "FAQ How do I open an editor on a file in the workspace?")

