

FAQ How do I create an Eclipse product?
=======================================

The information in this FAQ entry is @since Eclipse 3.1.

1.  Create a plugin for your product (File > New > Other > Plug-in Development > Plug-in Project).
2.  In your plugin manifest (plugin.xml), define an extension to the org.eclipse.core.runtime.products extension point. In the extension, define the product id, name, and description.
3.  Create a product configuration file (File > New > Other > Plugin Development > Product Configuration). Specify the product id as defined in your plugin manifest. On the "Configuration" tab, list all the plugins belonging to the product. You can also list branding information like the splash screen, icons, the name of your executable, etc.
4.  Use the Product export wizard (link from the product editor, or under File > Export) to build, package, and deploy your product.
5.  Launch the exe created by the product export (or run plain eclipse.exe with the -product argument to refer to your product).

Voila, your very own branded Eclipse product in five minutes or less!

See Also:
---------

*   [FAQ What is an Eclipse product?](./FAQ_What_is_an_Eclipse_product.md "FAQ What is an Eclipse product?")
*   Eclipse online article ["_Creating Product Branding_"](https://www.eclipse.org/articles/product-guide/guide.html)

