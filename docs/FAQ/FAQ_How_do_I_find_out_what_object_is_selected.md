

FAQ How do I find out what object is selected?
==============================================

The ISelectionService tracks all selection changes within the views and editors of a workbench window or page. By adding a listener to this service, you will be notified whenever the selection changes. Selections in views are typically returned as IStructuredSelection instances, and selections in editors typically implement ITextSelection. You should avoid any expensive computation from within a selection listener, because this event fires quite frequently as the user is moving around in the UI and typing in editors. A more efficient approach is to avoid adding a listener, and simply asking the selection service for the current selection when you need it.

You can also ask for the selection in a particular view by passing the view ID as a parameter to the getSelection method:

      IWorkbenchPage page = ...;
      //the current selection in the entire page
      ISelection selection = page.getSelection();
      //the current selection in the navigator view
      selection = page.getSelection(IPageLayout.ID\_RES\_NAV);
      //add a listener
      ISelectionListener sl = new ISelectionListener() {
         public void selectionChanged(IWorkbenchPart part, ISelection sel) {
            System.out.println("Selection is: " + sel);
         }
      };
      page.addSelectionListener(sl);
      //add a listener to selection changes only
      //in the navigator view
      page.addSelectionListener(sl, IPageLayout.ID\_RES\_NAV);

IWorkbenchPage implements ISelectionService directly. You can also access a selection service to track selection within a workbench window by using IWorkbenchWindow.getSelectionService.

See Also:
---------

*   [FAQ How do I find out what view or editor is selected?](./FAQ_How_do_I_find_out_what_view_or_editor_is_selected.md "FAQ How do I find out what view or editor is selected?")
*   [FAQ How do I find the active workbench page?](./FAQ_How_do_I_find_the_active_workbench_page.md "FAQ How do I find the active workbench page?")
*   [FAQ How do I make a view respond to selection changes in another view?](./FAQ_How_do_I_make_a_view_respond_to_selection_changes_in_another_view.md "FAQ How do I make a view respond to selection changes in another view?")
*   [FAQ How do I access the active project?](./FAQ_How_do_I_access_the_active_project.md "FAQ How do I access the active project?")

