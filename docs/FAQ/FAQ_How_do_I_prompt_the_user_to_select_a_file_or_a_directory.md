FAQ How do I prompt the user to select a file or a directory?
=============================================================

SWT provides native dialogs for asking the user to select a file (FileDialog) or a directory (DirectoryDialog). Both dialogs allow you to specify an initial directory (setFilterPath), and FileDialog also allows you to specify an initial selection (setFileName). Neither of these settings will restrict the user's ultimate choice as the dialogs allow the user to browse to another directory regardless of the filter path. FileDialog also allows you to specify permitted file extensions (setFilterExtensions), and the dialog will not let the user select a file whose extension does not match one of the filters. The following example usage of FileDialog asks the user to open an HTML file:

      FileDialog dialog = new FileDialog(shell, SWT.OPEN);
      dialog.setFilterExtensions(new String [] {"*.html"});
      dialog.setFilterPath("c:\\temp");
      String result = dialog.open();
  
By default, FileDialog allows the user to select only a single file, but with the SWT.MULTI style bit, it can be configured for selecting multiple files. In this case, you can obtain the result by using the getFileNames method. The returned file names will always be relative to the filter path, so be sure to prefix the filter path to the result to obtain the full path.
  

See Also:
---------

[FAQ How do I prompt the user to select a resource?](./FAQ_How_do_I_prompt_the_user_to_select_a_resource.md "FAQ How do I prompt the user to select a resource?")

