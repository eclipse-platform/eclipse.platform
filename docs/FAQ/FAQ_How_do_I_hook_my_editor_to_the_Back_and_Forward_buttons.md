

FAQ How do I hook my editor to the Back and Forward buttons?
============================================================

Each workbench page maintains a navigation history of interesting locations that have been visited in the page's editors. All editors, not only text editors, can contribute locations to this history, allowing the user to quickly jump between these locations by using the **Back** and **Forward** buttons on the toolbar. To enable this, your editor must implement INavigationLocationProvider. This interface is used to ask an editor to create an INavigationLocation object, which is a representation of the current editor state. When the user clicks the **Back** or **Forward** button, the previous or next location in the history restores its position using the restoreLocation method on INavigationLocation.

  
The contract that should be followed for an INavigationLocation is that when the user jumps in one direction, a subsequent jump in the opposite direction should take the user back to the starting point. To support this, your implementation of restoreLocation must add a history entry for the current location before restoring the old location.

You can imagine that this will quickly lead to duplication of entries if the user continues to jump backward and forward several times. This duplication is avoided by the mergeInto method on INavigationLocation. If the location to be merged is the same as or overlapping the receiver location, the method should merge the two entries and return true. If the locations don't overlap, it simply returns false.

  
A navigation location can also choose to support persistence. When an editor closes, any locations associated with that editor are asked to store their state in an IMemento. When the user jumps back to a location in an editor that has been closed, the location will be given the editor's IEditorInput object and the IMemento that was stored. Using this information, the location instance must be able to restore its state and navigate to its location in the editor. Note that you can easily obtain the editor instance from the input object by using IWorkbenchPage.findEditor(IEditorInput).

  
Now we know how to create and restore editor locations, but how are entries added to the navigation history in the first place? Anyone can mark an interesting location in an open editor by calling the markLocation method on INavigationHistory. Code that causes the cursor or selection to jump to another location in an editor should call this method both before and after performing the jump. As mentioned, implementations of restoreLocation should also mark the current location before restoring an old one. Regardless of whether the specific editor has any support for navigation history, markLocation will work. If the editor doesn't implement INavigationLocationProvider, a history entry will be added, allowing the user to jump back to that editor but without returning to any particular location. The following snippet shows an action that is added to the sample HTML editor. When the action is invoked, it will add the current cursor position to the navigation history:

      public class MarkLocationAction extends Action {
         private IEditorPart editor;
         public MarkLocationAction(IEditorPart editor) {
            super("Mark Location in History", null);
            this.editor = editor;
         }
         public void run() {
            IWorkbenchPage page = editor.getSite().getPage();
            page.getNavigationHistory().markLocation(editor);
         }
      }

