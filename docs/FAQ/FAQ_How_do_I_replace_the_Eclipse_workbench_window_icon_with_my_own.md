

FAQ How do I replace the Eclipse workbench window icon with my own?
===================================================================

The Eclipse workbench icon is defined by the Eclipse product. This file is specified in the about.ini file in the product plug-in's install directory, using the key windowImage if only a single 16×16 icon is provided, or windowImages if the product has both 16×16 and 32×32 icons. Note that these about.ini constants are defined by the IProductConstants interface in org.eclipse.ui.workbench.

As a debugging aid, many Eclipse developers hack the icon for their runtime workbench to make it easy to distinguish from the development workbench. This saves you from accidentally deleting your work while you try to reproduce some bug in a test environment. Simply check out the org.eclipse.platform plug-in from the Eclipse repository, and replace eclipse.gif with any other icon. Now, every runtime workbench will have that custom icon in the upper left-hand corner.

See Also:
---------

*   [FAQ Where can I find the Eclipse plug-ins?](./FAQ_Where_can_I_find_the_Eclipse_plug-ins.md "FAQ Where can I find the Eclipse plug-ins?")
*   [FAQ What is an Eclipse product?](./FAQ_What_is_an_Eclipse_product.md "FAQ What is an Eclipse product?")
*   [Eclipse online article _Creating Product Branding_](https://www.eclipse.org/articles/product-guide/guide.html)

