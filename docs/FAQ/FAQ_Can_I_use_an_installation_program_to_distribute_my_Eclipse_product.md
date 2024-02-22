

FAQ Can I use an installation program to distribute my Eclipse product?
=======================================================================

Most Eclipse-based commercial product offerings ship their own product plug-ins using a professional installation program, such as InstallShield, or using a free one, such as the Install Toolkit for Java (from IBM AlphaWorks). Such installation tools provide the flexibility of user-defined scripts that are run during installation to discover already installed Eclipse installations, to query the registry on Windows, and most important, to provide graceful uninstall capabilities.

For example, IBM's WebSphere Studio Device Developer is shipped as a set of plug-ins and embedded Java runtimes, together with the latest release of Eclipse, designed to install from a CD-ROM. The product can be installed stand alone or added to an existing IBM product, such as WebSphere Studio Application Developer. Doing the installation with a product like InstallShield allows for this kind of flexibility during the installation.

Once the product has been installed, it is recommended that the Eclipse Update Manager be used to check for updates and that an update site be used for delivering new features or feature upgrades to users. This is the way users of WebSphere Studio Device Developer can add support for embedded platforms that were added recently or that are provided by third parties.

See Also:
---------

*    [InstallShield](https://www.installshield.com)

The referenced IBM alphaWorks Install Toolkit for Java has not be available since April 9, 1998.

