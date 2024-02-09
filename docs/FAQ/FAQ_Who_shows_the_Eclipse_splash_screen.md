

FAQ Who shows the Eclipse splash screen?
========================================

The splash screen that appears during start-up is provided by the Eclipse product. On start-up, the platform looks for a file called splash.bmp in the product plug-in's install directory. The Eclipse launcher, such as eclipse.exe on Windows, specifies the name of the command to run for showing the splash screen using the -showsplash command-line argument. If you are not defining your own custom launcher, all you need to do is place the splash image in the product's install directory, and the launcher will find and open it.

See Also:
---------

*   [FAQ What is an Eclipse product?](./FAQ_What_is_an_Eclipse_product.md "FAQ What is an Eclipse product?")
*   Eclipse online article [_Creating Product Branding_](https://www.eclipse.org/articles/product-guide/guide.html)

