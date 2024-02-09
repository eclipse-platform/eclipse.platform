

FAQ Can other plug-ins add actions to my part's context menu?
=============================================================

They can, but only if you want them to. If you want other plug-ins to be able to add actions to your part's context menu, you need to do two things. First, you need to add a GroupMarker instance to your context menu with the special ID IWorkbenchActionConstants.MB_ADDITIONS. This placeholder tells the platform where actions contributed by other plug-ins should appear in the context menu.

  
Second, when it is created, you need to register with the platform your part's context menu by calling the IViewSite or IEditorSite method registerContextMenu. If it has more than one context menu, your part can contribute several menus by defining a unique ID for each one. By convention, a part that has only one context menu uses the part ID as the context menu ID. You should publish these context menu IDs in your plug-in's documentation so that other plug-in writers know what they are.

