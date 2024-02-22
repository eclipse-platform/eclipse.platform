

FAQ How do I find out the install location of a plug-in?
========================================================

You should generally avoid making assumptions about the location of a plug-in at runtime. To find resources, such as images, that are stored in your plug-in's install directory, you can use URLs provided by the Platform class. These URLs use a special Eclipse Platform protocol, but if you are using them only to read files, it does not matter.

The following snippet opens an input stream on a file called sample.gif located in a subdirectory, called icons, of a plug-in's install directory:

   Bundle bundle = Platform.getBundle(yourPluginId);
   Path path = new Path("icons/sample.gif");
   URL fileURL = FileLocator.find(bundle, path, null);
   InputStream in = fileURL.openStream();

If you need to know the file system location of a plug-in, you need to use FileLocator.resolve(URL). This method converts a platform URL to a standard URL protocol, such as HyperText Transfer Protocol (HTTP), or file. Note that the Eclipse Platform does not specify that plug-ins must exist in the local file system, so you cannot rely on this method's returning a file system URL under all circumstances in the future.

  

See Also:
---------

*   [FAQ Where do plug-ins store their state?](./FAQ_Where_do_plug-ins_store_their_state.md "FAQ Where do plug-ins store their state?")

