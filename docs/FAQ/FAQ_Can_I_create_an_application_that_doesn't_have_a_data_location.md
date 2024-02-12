

FAQ Can I create an application that doesn't have a data location?
==================================================================

Yes you can, if you are very careful. In Eclipse 3.0, the base Eclipse runtime was designed to be able to run without any data location at all. If you have a carefully crafted RCP application, you might be able to get away with not having a data location. To launch an Eclipse application with no data location at all, use the special -data @none command-line argument:

      eclipse -data @none -application your.app.id

If you do this, an error will occur if any plug-in attempts to access the platform instance location, including the plug-in metadata location. In other words, this configuration makes sense only for a tightly controlled application in which you are absolutely certain that the instance location will never be used.

One advantage of this approach is that multiple instances of your application can run simultaneously without forcing the user to pick a different data location for each one. For most RCP applications, this type of configuration is too constrictive. A better approach for applications that don't need to store any interesting state is to pick a random location in a scratch directory, such as the directory provided by System.getProperty("java.io.tmpdir"). This will ensure that your application does not fail if a plug-in is installed that does want to access the instance location.

  

See Also
---------

[FAQ\_How\_do\_I\_specify\_where\_application\_data\_is_stored?](./FAQ_How_do_I_specify_where_application_data_is_stored.md "FAQ How do I specify where application data is stored?")

