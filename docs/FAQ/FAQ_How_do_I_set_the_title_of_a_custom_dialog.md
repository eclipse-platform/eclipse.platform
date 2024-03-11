

FAQ How do I set the title of a custom dialog?
==============================================

If you have created your own subclass of the JFace Dialog class, you can set the dialog title by overriding the configureShell method:

      protected void configureShell(Shell shell) {
         super.configureShell(shell);
         shell.setText("My Dialog");
      }

The configureShell method can also be used to set the dialog menu bar image (Shell.setImage), the font (Shell.setFont), the cursor (Shell.setCursor), and other shell attributes.

