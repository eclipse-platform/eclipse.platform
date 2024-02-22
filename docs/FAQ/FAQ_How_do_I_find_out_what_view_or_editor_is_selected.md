FAQ How do I find out what view or editor is selected?
======================================================

To find out what view or editor is selected, use the IPartService. As with ISelectionService, you can add a listener to this service to track the active part or simply query it whenever you need to know. Note, saying that the part is _active_ does not imply that it has _focus_. If a dialog opens on top of the workbench window, the active part does not change, even though the active part loses focus. The part service will also notify you when parts are closed, hidden, brought to the top of a stack, and during other lifecycle events.

Two types of listeners can be added to the part service: IPartListener and the poorly named IPartListener2. You should always use this second one as it can handle part-change events on parts that have not yet been created because they are hidden in a stack behind another part. This listener will also tell you when a part is made visible or hidden or when an editor's input is changed:

	      IWorkbenchPage page = ...;
	      //the active part
	      IWorkbenchPart active = page.getActivePart();
	      //adding a listener
	      IPartListener2 pl = new IPartListener2() {
	         public void partActivated(IWorkbenchPartReference ref) {
	            System.out.println("Active: "+ref.getTitle());
	         }
	         ... other listener methods ...
	      };
	      page.addPartListener(pl);

IWorkbenchPage implements IPartService directly. You can also access a activation service by using IWorkbenchWindow.getPartService.

See Also:
---------

*   [FAQ How do I find out what object is selected?](./FAQ_How_do_I_find_out_what_object_is_selected.md "FAQ How do I find out what object is selected?")
*   [FAQ How do I find the active workbench page?](./FAQ_How_do_I_find_the_active_workbench_page.md "FAQ How do I find the active workbench page?")
*   [FAQ Why do the names of some interfaces end with the digit 2?](./FAQ_Why_do_the_names_of_some_interfaces_end_with_the_digit_2.md "FAQ Why do the names of some interfaces end with the digit 2?")

