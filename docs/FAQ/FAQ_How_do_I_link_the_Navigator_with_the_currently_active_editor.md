

FAQ How do I link the Navigator with the currently active editor?
=================================================================

The Navigator provides a view of the workspace and shows resources available on the file system. From the Navigator, files can be opened in an editor. When multiple editors are opened, it can be difficult to keep track of where the related resources are in the Navigator. For this reason, the Navigator has support to synchronize its tree view with the currently edited resource.

  

To link resources between the Navigator and the editors in the workbench, locate the **Link** button in the Navigator toolbar at the top of its UI. The **Link** button is an icon with two arrows pointing to each other. Move the mouse over it; hover help should read Link with Editor. This technique also applies to the JDT's Package Explorer.

  

  
If you are creating views with input linked to the selection in other views or editors, consider introducing a similar link option in your view's toolbar. The platform has been grappling with this issue of view linking for years, and we have found that letting the user decide is the only viable option. A view that always links its input will usually cause the user to lose context, as the view continually jumps around, scrolls, and changes its selection. Very few users will leave linking turned on all the time but will instead toggle it on and off again to find the item they are currently editing.

See Also:
---------

[FAQ\_How\_do\_I\_hide\_referenced\_libraries\_in\_the\_Package\_Explorer?](./FAQ_How_do_I_hide_referenced_libraries_in_the_Package_Explorer.md "FAQ How do I hide referenced libraries in the Package Explorer?")

