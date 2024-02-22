FAQ Can I add icons declared by my plugin.xml in the runtime JAR?
=================================================================

No. Statically declared plug-in icons are not meant to be in the runtime JAR because Eclipse wants to load plug-ins lazily. In other words, during loading of the platform, the platform loader reads only the plugin.xml file and will use the icons that are declared there. Only when the plug-in is really needed is the JAR opened; the class loader starts loading classes, and the plug-in is activated.

The structure of a plug-in roughly looks like this:

     plugin.xml         // used by platform loader
     icons/
         delete.gif     // icons used in the toolbar, and so on.
         create.jpg
     code.jar         // jar (class files + resources)

Plug-in activation is a process that requires considerable memory and central processing unit (CPU) cycles. To speed up load time of the platform, opening the runtime JAR is avoided for as long as possible.


Of course, images included in a runtime JAR can be used by plug-in code using standard Java resource-loading techniques, such as getResource on a given Java class or by using a class loader.

See Also
--------

[FAQ\_How\_do\_I\_add\_images\_and\_other\_resources\_to\_a\_runtime\_JAR_file?](./FAQ_How_do_I_add_images_and_other_resources_to_a_runtime_JAR_file.md "FAQ How do I add images and other resources to a runtime JAR file?")

  
[FAQ\_When\_does\_a\_plug-in\_get\_started?](./FAQ_When_does_a_plug-in_get_started.md "FAQ When does a plug-in get started?")

