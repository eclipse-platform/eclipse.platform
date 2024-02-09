

FAQ How do I add images and other resources to a runtime JAR file?
==================================================================

A plug-in is built using an Ant build script called build.xml. You create this script from a plugin.xml file by choosing from the context menu **Create Ant** Build File**. When it runs the build script, Ant includes settings from the** build.properties file. Open that file in an editor, and add an entry for the bin.includes variable. Add a pattern such as **images/*.gif**

to add all GIF files in the images directory.

  

Then, rerun the Ant build script to generate a runtime JAR including the resources you just specified. Check to see whether the created JAR includes the resources you intended.

  

  

See Also:
---------

\[\[FAQ\_Can\_I\_add\_icons\_declared\_by\_my\_%3Ctt%3Eplugin.xml%3C%2Ftt%3E\_in\_the\_runtime\_JAR%3F\]\]

