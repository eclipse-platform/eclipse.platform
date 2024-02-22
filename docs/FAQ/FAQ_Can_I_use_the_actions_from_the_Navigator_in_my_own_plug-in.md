

FAQ Can I use the actions from the Navigator in my own plug-in?
===============================================================

Yes. All the resource actions in the Navigator view, including **Copy**, **Move**, **Delete**, **Build**, and **Refresh**, are available as API. These actions are found in the org.eclipse.ui.actions package of the org.eclipse.ui.ide plug-in. These actions expect a selection of either IResource objects or IAdaptable objects that are able to adapt to IResource. You must either install the actions as selection change listeners, such as on a TreeViewer, or supply them with the selection before running them:

   IResource r = ...;//resource to delete
   IStructuredSelection ss = new StructuredSelection(r);
   DeleteResourceAction delete = new DeleteResourceAction(shell);
   delete.selectionChanged(ss);
   delete.run();

  

See Also:
---------

[FAQ How do I use IAdaptable and IAdapterFactory?](./FAQ_How_do_I_use_IAdaptable_and_IAdapterFactory.md "FAQ How do I use IAdaptable and IAdapterFactory?")

