

FAQ How do I display search results?
====================================

(This article seems to describe the now deprecated "classic" search)

Marker-based search results can be displayed in the Search Results view provided by the org.eclipse.search plug-in. To do this, you first need to add org.eclipse.search plugin to your plugin.xml dependencies. Then make sure that the Search Results view is created and then obtain a reference via the NewSearchUI class:

	   NewSearchUI.activateSearchResultView();
	   ISearchResultViewPart view = NewSearchUI.getSearchResultView();

  
Before you begin adding search results to the view, you need to call ISearchResultView.searchStarted. This method lets the view know that the series of matches about to be added belong to a single search query. This method takes the following:

*   IActionGroupFactory, a factory object for creating the actions that will

appear in the context menu when a search result is selected.

*   String, the label to use in the view title bar when there is exactly one

search result. This label should describe the search thoroughly because it will also appear in the search history list that allows the user to add old searches back to the view.

*   String, the same label as the preceding, but for multiple search results. The

string should contain the pattern {0}, which will be replaced with the exact number of occurrences.

*   ImageDescriptor, the image to use for this group of results. This will

also appear in the search history drop-down list. If you don't provide one, a default icon will be used.

*   String, the ID of the Search dialog page that generated this

set of search results. This is the ID attribute from the search page extension declaration.

*   ILabelProvider, the label provider to use for displaying each search result. If

not provided, a reasonable default will be used.

*   IAction, the action that will cause your search result to be opened in an

editor.

*   IGroupByKeyComputer (described in the next paragraph).
*   IRunnableWithProgress, a runnable that will execute the search query

over again. This can be the exact runnable executed from the Search dialog.

  

The Search Results view shows results in groups, where each line in the view is a single group. A group typically corresponds to a logical unit, such as a file or a Java method, where the match was found. This serves to reduce clutter in the view so that a large number of results can be aggregated into a smaller space. The IGroupByKeyComputer object provided in the searchStarted method is used to map from search results to the group that corresponds to each result. If you don't want to group your search results, you don't need to provide this object.

  
Once the search has been started, each search result is added to the view by using the addMatch method. This method takes a description string, a resource handle, the search result marker, and an object that represents the group that the result belongs to. A typical grouping is to use the file as the group identifier. That way, all search results for a given file will be aggregated together in the Search Results view. If you don't want to group search results at all, use the marker itself as the group marker.

  
Finally, when you have finished adding search results, call the method searchFinished on the Search Results view. This method must be called in all circumstances, including failure and cancellation, so it is a good idea to put it in a finally block at the end of your search operation.

  

See Also:
---------

[FAQ How do I write a Search dialog?](./FAQ_How_do_I_write_a_Search_dialog.md "FAQ How do I write a Search dialog?")

[FAQ How do I implement a search operation?](./FAQ_How_do_I_implement_a_search_operation.md "FAQ How do I implement a search operation?")

