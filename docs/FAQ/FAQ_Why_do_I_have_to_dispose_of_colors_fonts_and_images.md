

FAQ Why do I have to dispose of colors, fonts, and images?
==========================================================

This question was asked so often that the SWT team wrote an article to explain why. The Eclipse online article [Managing Operating System Resources, by Carolyn McLeod and Steve Northover](https://www.eclipse.org/articles/swt-design-2/swt-design-2.html) describes SWT's philosophy on resource management and defends its reasons for not relying on the Java garbage collector for disposing of unused resources. The philosophy, in short, is this: If you create it, you dispose of it. The capsule summary of the reasoning is that the specification for Java finalization is too weak to reliably support management of operating system resources. This is also why database connections, sockets, file handles, and other heavyweight resources are not handled by the Java garbage collector. If you are still not convinced, read the article.

See Also:
---------

*   [FAQ\_How\_do\_I\_use\_image\_and\_font\_registries?](./FAQ_How_do_I_use_image_and_font_registries.md "FAQ How do I use image and font registries?")

