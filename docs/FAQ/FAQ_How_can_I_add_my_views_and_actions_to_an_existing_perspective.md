

FAQ How can I add my views and actions to an existing perspective?
==================================================================

Use the org.eclipse.ui.perspectiveExtensions extension point. This extension point allows a third party to define the position of views in another plug-in's perspective. This extension point can also be used to add actions to the menus and toolbars. An interesting attribute of this extension point is that it is purely declarative. No Java code is associated with a perspective extension; it is purely XML markup. The mechanics of writing perspective extensions are well described in the _Platform Plug-in Developer's Guide_.

See Also:
---------

*   org.eclipse.ui.perspectiveExtensions (See [Platform Plug-in Developer's Guide](https://help.eclipse.org/help31/index.jsp))
*   [Using Perspectives in the Eclipse UI](https://www.eclipse.org/articles/using-perspectives/PerspectiveArticle.html)

