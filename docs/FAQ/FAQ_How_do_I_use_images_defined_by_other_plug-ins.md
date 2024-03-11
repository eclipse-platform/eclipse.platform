

FAQ How do I use images defined by other plug-ins?
==================================================

In general, you should not rely on images defined in other plug-ins as they could be changed or removed at any time. The exception to this rule is an image that has been declared by a plug-in as an API. The convention is to create an interface called ISharedImages that defines IDs for all images that you want to declare as API. Several plug-ins, including org.eclipse.ui.workbench, org.eclipse.jdt.ui, and org.eclipse.team.ui, declare such an ISharedImages class. Here is an example snippet that options the folder icon from the workbench's shared image API:

      IWorkbench workbench = PlatformUI.getWorkbench();
      ISharedImages images = workbench.getSharedImages();
      Image image = images.getImage(ISharedImages.IMG_OBJ_FOLDER);

If you want to share images from your plug-in as API, you should define your own ISharedImages class with constants for all the images you want to make public.

See Also:
---------

*   [FAQ How do I create an image registry for my plug-in?](./FAQ_How_do_I_create_an_image_registry_for_my_plug-in.md "FAQ How do I create an image registry for my plug-in?")

