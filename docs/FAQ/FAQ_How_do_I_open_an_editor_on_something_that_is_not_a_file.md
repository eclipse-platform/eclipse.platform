

FAQ How do I open an editor on something that is not a file?
============================================================

Since 3.3 you can use the new EFS support to open an text editor on a file store that's backed by any kind of EFS using `[IDE.openEditorOnFileStore(IWorkbenchPage, IFileStore)](https://help.eclipse.org/stable/nftopic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/ui/ide/IDE.html#openEditorOnFileStore(org.eclipse.ui.IWorkbenchPage,%20org.eclipse.core.filesystem.IFileStore))`.

Most editors will accept as input either an IFileEditorInput or an IStorageEditorInput. The former can be used only for opening files in the workspace, but the latter can be used to open a stream of bytes from anywhere. If you want to open a file on a database object, remote file, or other data source, IStorage is the way to go. The only downside is that this is a read-only input type, so you can use it only for viewing a file, not editing it. To use this approach, implement IStorage so that it returns the bytes for the file you want to display. Here is an IStorage that returns the contents of a string:

      class StringStorage implements IStorage {
        private String string;
       
        StringStorage(String input) {
          this.string = input;
        }
       
        public InputStream getContents() throws CoreException {
          return new ByteArrayInputStream(string.getBytes());
        }
       
        public IPath getFullPath() {
          return null;
        }
       
        public Object getAdapter(Class adapter) {
          return null;
        }
       
        public String getName() {
          int len = Math.min(5, string.length());
          return string.substring(0, len).concat("..."); //$NON-NLS-1$
        }
       
        public boolean isReadOnly() {
          return true;
        }
      }

The class extends PlatformObject to inherit the standard implementation of IAdaptable, which IStorage extends. The getName and getFullPath methods can return null if they are not needed. In this case, we've implemented getName to return the first five characters of the string.

The next step is to create an IStorageEditorInput implementation that returns your IStorage object:

 

       class StringInput implements IStorageEditorInput {
          private IStorage storage;
          StringInput(IStorage storage) {this.storage = storage;}
          public boolean exists() {return true;}
          public ImageDescriptor getImageDescriptor() {return null;}
          public String getName() {
             return storage.getName();
          }
          public IPersistableElement getPersistable() {return null;}
          public IStorage getStorage() {
             return storage;
          }
          public String getToolTipText() {
             return "String-based file: " + storage.getName();
          }
          public Object getAdapter(Class adapter) {
            return null;
          }
       }

Again, many of the methods here are optional. The getPersistable method is used for implementing persistence of your editor input, so the platform can automatically restore your editor on start-up. Here, we've implemented the bare essentials: the editor name, and a tool tip.

The final step is to open an editor with this input. This snippet opens the platform's default text editor on a given string:

 

       IWorkbenchWindow window = ...;
       String string = "This is the text file contents";
       IStorage storage = new StringStorage(string);
       IStorageEditorInput input = new StringInput(storage);
       IWorkbenchPage page = window.getActivePage();
       if (page != null)
          page.openEditor(input, "org.eclipse.ui.DefaultTextEditor");

See Also:
---------

*   [FAQ How do I open an editor programmatically?](./FAQ_How_do_I_open_an_editor_programmatically.md "FAQ How do I open an editor programmatically?")
*   [FAQ How do I open an editor on a file outside the workspace?](./FAQ_How_do_I_open_an_editor_on_a_file_outside_the_workspace.md "FAQ How do I open an editor on a file outside the workspace?")

