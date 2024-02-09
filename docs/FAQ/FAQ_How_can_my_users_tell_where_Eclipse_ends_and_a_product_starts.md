

FAQ How can my users tell where Eclipse ends and a product starts?
==================================================================

You cannot see where Eclipse ends and a product starts, and this is intentional. The platform itself is written entirely as a set of plug-ins, and the product plug-ins simply join the “soup of swimming plug-ins.” To bring some order to the chaos, the platform does maintain a history of configurations. When a new plug-in is installed, a new configuration is created. To obtain insights into the current configuration of the platform and its update history, consult either **Help > About... > Configuration Details** or the Update Manager. There you can discover the installed plug-ins that are part of the Eclipse Platform and those that were installed afterward.

Having said that, an Eclipse-based product can exert a certain amount of branding on the appearance of the final application. In particular, a product will usually replace the workbench window icon and splash screen. The product can also configure a set of preferences that will ship as the defaults for users of that product. See Chapter 14 for more details on productizing an Eclipse offering.

See Also:
---------

*   [FAQ What is an Eclipse product?](./FAQ_What_is_an_Eclipse_product.md "FAQ What is an Eclipse product?")

