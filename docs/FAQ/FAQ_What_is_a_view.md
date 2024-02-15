

FAQ What is a view?
===================

Views are one of the two kinds of parts that make up a workbench window. At their most basic, views are simply a subclass of the SWT Composite class, containing arbitrary controls below a title bar. The title bar contains the view name, an area for toolbar buttons, and one or two drop-down menus. The drop-down menu on the upper left is simply the standard shell menu with actions for moving, resizing, and closing the view. The menu on the upper right and the button area are the view's _action bar_ and may contain arbitrary actions defined by the implementer of that view.

A view interacts with the rest of the workbench via its _site_. Browse through the interfaces IViewSite, IWorkbenchPartSite, and IWorkbenchSite to see what site services are available to a view.

See Also:
---------

*   [FAQ What is a viewer?](./FAQ_What_is_a_viewer.md "FAQ What is a viewer?")
*   [FAQ Pages, parts, sites, windows: What is all this stuff?](./FAQ_Pages_parts_sites_windows_What_is_all_this_stuff.md "FAQ Pages, parts, sites, windows: What is all this stuff?")
*   [FAQ What is the difference between a view and a viewer?](./FAQ_What_is_the_difference_between_a_view_and_a_viewer.md "FAQ What is the difference between a view and a viewer?")
*   [FAQ What is the difference between a view and an editor?](./FAQ_What_is_the_difference_between_a_view_and_an_editor.md "FAQ What is the difference between a view and an editor?")

