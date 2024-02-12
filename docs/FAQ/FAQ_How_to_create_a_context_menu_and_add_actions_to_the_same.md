

FAQ How to create a context menu and add actions to the same?
=============================================================

**To create a Context Menu for your View or Editor do the following :**

  
Step 1. Use popupMenus Extension point as explained in the Help of Eclipse. [Creating Popup Menus](https://help.eclipse.org/help30/topic/org.eclipse.platform.doc.isv/guide/workbench_basicext_popupMenus.htm)

Step 2. You need to also register the context menu to the ViewSite or to the EditorSite which will contain the context menu. This is done using the registerContextMenu function available with ISite.

The following code snippet explains how to register the context menu to your View.

      private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		Menu menu = menuMgr.createContextMenu(<Your Viewer>.getControl());
		<Your Viewer>.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, <Your Viewer>);
	}
 

This function needs to be implemented and then called from createPartControl().

--[Annamalai.chockalingam.gmail.com](/index.php?title=User:Annamalai.chockalingam.gmail.com&action=edit&redlink=1 "User:Annamalai.chockalingam.gmail.com (page does not exist)") 07:24, 21 September 2006 (EDT)

