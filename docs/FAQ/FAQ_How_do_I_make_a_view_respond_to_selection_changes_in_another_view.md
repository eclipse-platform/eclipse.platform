

FAQ How do I make a view respond to selection changes in another view?
======================================================================

Views can be made to respond to selection changes in other views with org.eclipse.ui.ISelectionService. A selection service allows views to work together without having to couple their views tightly. It also adds flexibility and extensibility to your design.

Say you have one view that shows a list of books and another view that displays the chapters within a book. When the user selects a book in the first view, you want to display the chapters of that book in the second view. Now say that another plug-in is installed that provides a graphical Book view and a Metrics view that displays various metrics about a selected book. If your original views were linked using explicit coupling, selection changes in your Books view would not activate the Metrics view. Conversely, selection changes in the graphical Books view would not correctly update your Chapters view. Using the selection service gives an opportunity for other plug-ins to connect to your view by listening to your selection changes or providing selections that your views can respond to.

Follow three simple steps to make use of an ISelectionService in your views. First, find an appropriate selection service. Two selection services are available to a view: window selection and page selection. The first is used for tracking selection changes in an entire workbench window, and the second is constrained to selection changes within a single page. Typically, you want to track selection changes only within the current page. Because only one page is visible at a time anyway, responding to selection changes in other pages is generally a waste of effort. The window-selection service is accessible via IWorkbenchWindow.getSelectionService. The page-selection service extends the ISelectionService interface and is available from IWorkbenchPage.

Second, register the view-selection provider with the selection service. The view that wants to broadcast selection changes simply has to register itself with the ISelectionService by supplying an implementation of org.eclipse.jface.viewers ISelectionProvider. In most cases, your view will contain a JFace viewer, all of which implement ISelectionProvider directly. If your view does not contain a viewer, you will have to implement the ISelectionProvider interface directly. Here is a sample of a view's createPartControl method, which creates a viewer and then registers it with the page-selection service:

      public void createPartControl(Composite parent) {
         int style = SWT.SINGLE | SWT.H\_SCROLL | SWT.V\_SCROLL;
         viewer = new TableViewer(parent, style);
         getSite().setSelectionProvider(viewer);
         ...
      }

  
The final step is to register a selection listener with the selection service. The view that wants to respond to selection changes must register an implementation of org.eclipse.ui ISelectionListener with the selection service. The following example defines the Chapters view described earlier. This view responds to selection changes where the selection is a book and displays the chapters of that book. It is important for your selection listener to ignore selections that it does not understand. The selection service will be broadcasting all kinds of selection changes that are not relevant to your view, so you need to listen selectively.

      public class ChaptersView extends ViewPart {
         private TableViewer viewer;
         ISelectionListener listener = new ISelectionListener() {
            public void selectionChanged(IWorkbenchPart part, ISelection sel) {
               if (!(sel instanceof IStructuredSelection))
                  return;
               IStructuredSelection ss = (IStructuredSelection) sel;
               Object o = ss.getFirstElement();
               if (o instanceof Book)
                  viewer.setInput(ss.size()==1 ? o : null);
            }
         };
         public void createPartControl(Composite parent) {
            getSite().getPage().addSelectionListener(listener);
         }
         public void dispose() {
            getSite().getPage().removeSelectionListener(listener);
         }
      }

Again, note how this example completely ignores selections that don't contain books. If the selection contains a single book, the view will display its chapters. If the selection contains several books, it will display nothing. Also note in this example that the view is responsible for removing the selection listener when it closes. As a general rule with listeners, the code for adding a listener should never be very far from the code for removing the listener. If you forget to remove your listener when the view is closed, your selection listener will cause errors later on.

  

See Also:
---------

*   [FAQ How do I find out what object is selected?](./FAQ_How_do_I_find_out_what_object_is_selected.md "FAQ How do I find out what object is selected?")

