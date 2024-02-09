

FAQ How do I implement a search operation?
==========================================

A search operation is initiated from the performAction method on a search page. The search can run either in a blocking manner, thus preventing the user from doing further work until the search is done, or in the background. To run the search in a blocking manner, use the IRunnableContext available from the ISearchPageContainer instance:

      class SearchSizePage extends DialogPage 
      implements ISearchPage {
         private ISearchPageContainer container;
         public boolean performAction() {
            // ... validate input ...
            IRunnableWithProgress query = ...;//the query object
            container.getRunnableContext().run(true, true, query);
            return true;
         }
         public void setContainer(ISearchPageContainer spc) {
            this.container = spc;
         }
      }

To run your query in the background, create and schedule a subclass of Job. Regardless of whether the search is run in the foreground or the background, the mechanics of the search operation itself will usually be the same.

  
If your search is operating on files in the workspace, you should ensure that changes are batched to prevent autobuilds every time a search result is created. Do this by making your operation subclass WorkspaceModifyOperation in the blocking case or WorkspaceJob in the nonblocking case. For the rest of this FAQ, we'll assume that you're writing a search on the workspace.

  
The purpose of your search operation is to locate the files that match the search parameters and to generate search result markers for each match. One common method of doing this is to use a resource visitor. Here is the general structure of a simple search operation:

      class SearchOperation extends WorkspaceModifyOperation
         implements IResourceProxyVisitor {
         public void execute(IProgressMonitor monitor) {
            ResourcesPlugin.getWorkspace().getRoot().accept(
               this, IResource.DEPTH_INFINITE);
         }
         protected boolean isMatch(IFile file) {
            ... test match criteria ...
         }
         public boolean visit(IResourceProxy proxy) {
            if (proxy.getType() == IResource.FILE) {
               IFile file = (IFile) proxy.requestResource();
               if (isMatch(file)) 
                  file.createMarker(SearchUI.SEARCH_MARKER);
            }
            return true;
         }
      }

If your search is located within a specific portion of the file, you should fill in the appropriate attributes on the search result marker (LINE_NUMBER, CHAR_START, and CHAR_END from IMarker). None of these attributes is required; in some cases, a search can simply identify an entire file.


See Also:
---------

[FAQ\_How\_do\_I\_write\_a\_Search_dialog?](./FAQ_How_do_I_write_a_Search_dialog.md "FAQ How do I write a Search dialog?")

[FAQ\_How\_do\_I\_display\_search\_results?](./FAQ_How_do_I_display_search_results.md "FAQ How do I display search results?")

[FAQ\_How\_do\_I\_prevent\_builds\_between\_multiple\_changes\_to\_the_workspace?](./FAQ_How_do_I_prevent_builds_between_multiple_changes_to_the_workspace.md "FAQ How do I prevent builds between multiple changes to the workspace?")

\[\[FAQ\_Why\_don%26%23146%3Bt\_my\_markers\_appear\_in\_the\_editor%26%23146%3Bs\_vertical\_ruler%3F\]\]

