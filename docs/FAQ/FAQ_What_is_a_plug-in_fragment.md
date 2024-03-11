

FAQ What is a plug-in fragment?
===============================

Sometimes it is useful to make part of a plug-in optional, allowing it to be installed, uninstalled, or updated independently from the rest of the plug-in. For example, a plug-in may have a library that is specific to a particular operating system or windowing system or a language pack that adds translations for the plug-in's messages. In these situations, you can create a fragment that is associated with a particular host plug-in. On disk, a fragment looks almost exactly the same as a plug-in, except for a few cosmetic differences.

*   The manifest is stored in a file called fragment.xml instead of plugin.xml.
*   The top-level element in the manifest is called fragment and has two extra attributes, plugin-id and plugin-version, for specifying the ID and version number of the host plug-in.
*   The fragment manifest does not need its own requires element. The fragment will automatically inherit the requires element of its host plug-in. It can add requires elements if it needs access to plug-ins that are not required by the host plug-in.

Apart from these differences, a fragment appears much the same as a normal plug-in. A fragment can specify libraries, extensions, and other files. When it is loaded by the platform loader, a fragment is logically, but not physically, merged into the host plug-in. The end result is exactly the same as if the fragment's manifest were copied into the plug-in manifest, and all the files in the fragment directory appear as if they were located in the plug-in's install directory. Thus, a runtime library supplied by a fragment appears on the classpath of its host plug-in. In fact, a Java class in a fragment can be in the same package as a class in the host and will even have access to package-visible methods on the host's classes. The methods find and openStream on Plugin, which take as a parameter a path relative to the plug-in's install directory, can be used to locate and read resources stored in the fragment install directory.

  

  

  

See Also:
---------

[FAQ What is a plug-in?](./FAQ_What_is_a_plug-in.md "FAQ What is a plug-in?")

