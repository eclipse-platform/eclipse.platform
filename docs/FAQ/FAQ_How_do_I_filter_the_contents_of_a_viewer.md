

FAQ How do I filter the contents of a viewer?
=============================================

You do this with a ViewerFilter. ViewerFilters are added to a structured viewer by using the addFilter method. When a filter is added, it defines a subset of the original elements to be shown. Writing your own filter is trivial: Subclass ViewerFilter and override the method select. This method returns true if the element should be shown and false if it should be hidden. If more than one filter is added, the viewer will show only their intersection; that is, all filters must return true in their select method for the element to be shown.

One final tidbit of useful information: StructuredViewer has a convenience method, resetFilters, for removing all of a viewer's filters. This is more efficient than removing filters one at a time as each time a filter is added or removed, the entire viewer needs to be refreshed.

See Also:
---------

*   [FAQ What is a viewer?](./FAQ_What_is_a_viewer.md "FAQ What is a viewer?")
*   [FAQ How do I use properties to optimize a viewer?](./FAQ_How_do_I_use_properties_to_optimize_a_viewer.md "FAQ How do I use properties to optimize a viewer?")

