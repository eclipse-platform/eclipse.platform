

FAQ How do I find all the plug-ins that contribute to my extension point?
=========================================================================

Assuming that you used an element attribute named class to encode the name of the class that your contributors have to provide, you can obtain a list of all the plug-in classes that contribute to your extension point by using the following piece of code:

      IExtensionRegistry reg = Platform.getExtensionRegistry();
      IExtensionPoint ep = reg.getExtensionPoint(extensionID);
      IExtension[] extensions = ep.getExtensions();
      ArrayList contributors = new ArrayList();
      for (int i = 0; i < extensions.length; i++) {
         IExtension ext = extensions[i];
         IConfigurationElement[] ce = 
            ext.getConfigurationElements();
         for (int j = 0; j < ce.length; j++) {
            Object obj = ce[j].createExecutableExtension("class");
            contributors.add(obj);
         }
      }

From a given extension point, it is straightforward to get a list of extensions that contribute to it. For each extension, we create an executable extension using the value of the class property. We save all the executable extensions in an array list and return it.

How to get all IConfigurationElement objects more simply?
---------------------------------------------------------

You can also get all IConfigurationElement objects without retrieving each extension by using following code:

    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement[] elements = reg.getConfigurationElementsFor("Extension Point ID");

See Also:
---------

*   [FAQ How do I declare my own extension point?](./FAQ_How_do_I_declare_my_own_extension_point.md "FAQ How do I declare my own extension point?")

