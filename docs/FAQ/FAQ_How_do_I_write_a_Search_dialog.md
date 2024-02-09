

FAQ How do I write a Search dialog?
===================================

You can add custom search pages to the Eclipse Search dialog by adding an extension to the org.eclipse.search.searchPages extension point. This page, which must implement the ISearchPage interface, should have input fields for whatever search criteria you want to allow. Figure 16.1 shows an example search page that allows the user to search for files based on their size.

<img src=../images/size_search.png> **Figure 16.1** The Search dialog showing our example Size Search page

  
The code for creating the dialog page is much the same as for any other group of SWT widgets. The one caveat is that your createControl method must call setControl after your widget is created to let the dialog know what your top-level control is. Complete source code for the dialog is included in the FAQ Examples plug-in.

  
When the **Search** button is clicked, the performAction method is called on your search page. This method should perform the search and return true if the search was successful. If the user enters invalid search parameters, you should present a dialog explaining the error, and return false from the performAction method. Returning false causes the dialog to remain open so the user can fix the search parameters.

  
Your query page can, optionally, include a standard area allowing the user to specify a search scope. This scope can be the entire workspace, the selected resources, the selected projects, or a user-defined working set. If you want this scope area to appear in your page, include the attribute showScopeSection = "true" in your search page extension declaration. If you do include this, you should consult the chosen scope via the ISearchPageContainer instance that is passed to your page and make sure that your search operation honors the value of the scope.

  
Note that you are not restricted to creating queries based on files. The search dialog has pages for finding plug-ins and help contents as well. Other plug-ins are available on the Web for performing Bugzilla queries, news queries, and more. However, the infrastructure for presenting results in the Search Results view is limited to file-based searches. If your search is not operating on files, you will need to create your own view or dialog for presenting search results.

  

See Also:
---------

[FAQ\_How\_do\_I\_implement\_a\_search_operation?](./FAQ_How_do_I_implement_a_search_operation.md "FAQ How do I implement a search operation?")

[FAQ\_How\_do\_I\_display\_search\_results?](./FAQ_How_do_I_display_search_results.md "FAQ How do I display search results?")

