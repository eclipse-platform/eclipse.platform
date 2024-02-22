

FAQ What is a viewer?
=====================

The purpose of a viewer is to simplify the interaction between an underlying model and the widgets used to present elements of that model. A viewer is not used as a high-level replacement for an SWT widget but as an adapter that sits beside an SWT widget and automates some of the more mundane widget-manipulation tasks, such as adding and removing items, sorting, filtering, and refreshing.

A viewer is created by first creating an SWT widget, constructing the viewer on that widget, and then setting its content provider, label provider, and input. This snippet is from the BooksView class in the FAQ Examples plug-in, which creates a table viewer to display a library of books:

      int style = SWT.MULTI | SWT.H\_SCROLL | SWT.V\_SCROLL;
      Table table = new Table(parent, style);
      TableViewer viewer = new TableViewer(table);
      viewer.setContentProvider(new BookshelfContentProvider());
      viewer.setLabelProvider(new BookshelfLabelProvider());
      viewer.setInput(createBookshelf());

In general, JFace viewers allow you to create a model-view-controller (MVC) architecture. The view is the underlying SWT widget, the model is specified by the framework user, and the JFace viewer and its associated components form the controller. The viewer input is a model element that seeds the population of the viewer.

![](https://github.com/eclipse-platform/eclipse.platform/tree/master/docs/FAQ/images/Package_explorer.jpg)

JDT Package Explorer

The JDT Package Explorer uses a TreeViewer to display the contents of a workspace, represented as a JavaModel.


See Also:
---------

*   [FAQ What are content and label providers?](./FAQ_What_are_content_and_label_providers.md "FAQ What are content and label providers?")
*   [FAQ How do I sort the contents of a viewer?](./FAQ_How_do_I_sort_the_contents_of_a_viewer.md "FAQ How do I sort the contents of a viewer?")
*   [FAQ How do I filter the contents of a viewer?](./FAQ_How_do_I_filter_the_contents_of_a_viewer.md "FAQ How do I filter the contents of a viewer?")
*   [FAQ What is a view?](./FAQ_What_is_a_view.md "FAQ What is a view?")
*   [FAQ What is the difference between a view and a viewer?](./FAQ_What_is_the_difference_between_a_view_and_a_viewer.md "FAQ What is the difference between a view and a viewer?")

