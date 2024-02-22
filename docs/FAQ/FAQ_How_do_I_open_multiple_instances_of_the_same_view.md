

FAQ How do I open multiple instances of the same view?
======================================================

Eclipse 3.0 lifts the restriction of allowing only a single instance of each view type. You can now open any number of copies of a given view. Each copy needs a secondary ID to disambiguate the view reference. Here is a snippet that opens two copies of the Books view from the FAQ Examples plug-in:

      IWorkbenchPage page = ...;
      String id = "org.eclipse.faq.examples.BooksView";
      page.showView(id, "1", IWorkbenchPage.VIEW_VISIBLE);
      page.showView(id, "2", IWorkbenchPage.VIEW_ACTIVATE);

The first parameter is the view ID from the extension definition in the plugin.xml file. The second parameter is an arbitrary string that is used to identify that particular copy of the view. Finally, the third parameter is used to specify whether the view should be created but not made visible (VIEW_CREATE), created and made visible but not made active (VIEW_VISIBLE), or created and made visible and active (VIEW_ACTIVATE).

  
Once multiple copies of a view exist, they can be located by using the method IWorkbenchPage.findViewReference, where the primary and secondary view IDs are passed as a parameter.

