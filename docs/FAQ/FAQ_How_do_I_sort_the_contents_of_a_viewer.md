

FAQ How do I sort the contents of a viewer?
===========================================

Structured viewers are sorted by plugging in an instance of ViewerSorter, using the StructuredViewer.setSorter method. For simple sorting based on label text, use the generic ViewerSorter class itself, optionally supplying a java.text.Collator instance to define how strings are compared. For more complex comparisons, you'll need to subclass ViewerSorter. You can override the category method to divide the elements up into an ordered set of categories, leaving the text collator to sort the elements within each category. For complete customization of sorting, override the compare method, which acts much like the standard java.util.Comparator interface.

See Also:
---------

*   [FAQ What is a viewer?](./FAQ_What_is_a_viewer.md "FAQ What is a viewer?")
*   [FAQ How do I use properties to optimize a viewer?](./FAQ_How_do_I_use_properties_to_optimize_a_viewer.md "FAQ How do I use properties to optimize a viewer?")

