

FAQ What is an Eclipse product?
===============================

Strictly speaking, a product is an extension to the extension point called org.eclipse.core.runtime.products. The purpose of a product is to define application-specific branding on top of a configuration of Eclipse plug-ins. Minimally, a product defines the ID of the application it is associated with and provides a name, description, and unique ID of its own. A product also stores a table of additional properties, where the UI stores information such as the application window icon and the all-important blurb in the **Help > About...** dialog. It is quite possible to run Eclipse without a product, but if you want to customize the appearance of Eclipse for your particular application, you should define one.

Because more than one product can be installed at a given time, the main product is singled out in a special marker file called .eclipseproduct in the Eclipse install directory. This file denotes the name, ID, and version number of the main product that will be used. The product in turn is a plug-in in the plugins directory, which includes product branding elements, such as the splash screen and workbench window icons.

For more details, see the methods declared by the IProduct interface defined in the org.eclipse.core.runtime plug-in. IProductConstants in the org.eclipse.ui.workbench defines the keys of product properties that are of interest to Eclipse products having user interfaces.

See Also:
---------

*   [FAQ How do I create an Eclipse product?](./FAQ_How_do_I_create_an_Eclipse_product.md "FAQ How do I create an Eclipse product?")
*   [FAQ What is a configuration?](./FAQ_What_is_a_configuration.md "FAQ What is a configuration?")
*   Eclipse online article ["_Creating Product Branding_"](https://www.eclipse.org/articles/product-guide/guide.html)

