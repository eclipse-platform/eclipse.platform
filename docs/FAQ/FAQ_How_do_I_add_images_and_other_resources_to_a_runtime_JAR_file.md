

FAQ How do I add images and other resources to a runtime JAR file?
==================================================================

A plug-in is built using an Ant build script called build.xml. You create this script from a plugin.xml file by choosing from the context menu **Create Ant** Build File**. When it runs the build script, Ant includes settings from the** build.properties file. Open that file in an editor, and add an entry for the bin.includes variable. Add a pattern such as **images/*.gif**

to add all GIF files in the images directory.

  

Then, rerun the Ant build script to generate a runtime JAR including the resources you just specified. Check to see whether the created JAR includes the resources you intended.

  

  

See Also:
---------

[FAQ Can I add icons declared by my plugin.xml in the runtime JAR?](./FAQ_Can_I_add_icons_declared_by_my_plugin_xml_in_the_runtime_JAR.md)



