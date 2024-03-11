

FAQ When do I need to write a plug-in install handler?
======================================================

Writing a plug-in install handler is appropriate for most professional applications. Doing so is not only a comfort that most end users will expect but also allows you to perform custom installation, such as

*   Moving data to other locations in the file system outside the install directory. This allows data to be shared by multiple applications or by multiple versions of your application.
*   Reading or saving values in the Windows registry.
*   Asking the user to confirm licensing terms.
*   Searching for installed components, such as ActiveX or database drivers, that your application requires and installing them, if necessary.

When creating a feature using the PDE wizard, you get the option to specify an optional feature-specific install handler.

See Also:
---------

*   [FAQ Can I use an installation program to distribute my Eclipse product?](./FAQ_Can_I_use_an_installation_program_to_distribute_my_Eclipse_product.md "FAQ Can I use an installation program to distribute my Eclipse product?")

