

FAQ When does a plug-in get started?
====================================

A plug-in gets started when the user needs it. In UI design, an often cited rule is that the screen belongs to the user. A program should not make changes on the screen that the user didn't somehow initiate. Making users feel that they are in control of what is happening builds their confidence in the UI and results in a much pleasanter user experience. This rule is followed by the Eclipse UI, but the underlying principle has been applied to a much broader scope. In Eclipse, one of the goals is to have the screen, the CPU, and the memory footprint belong to the user; that is, the CPU should not be doing things the user didn't ask it to do, and memory should not be bloated with functions that the user may never need.

This principle is enforced in the Eclipse Platform through lazy plug-in activation. Plug-ins are activated only when their functionality has been explicitly invoked by the user. In theory, this results in a relatively small start-up time and a memory footprint that starts small and grows only as the user begins to invoke more and more functionality.

The extension point mechanism plays an important role in lazy activation. Each plug-in can be viewed as having a _declarative_ section and a code section. The declarative part is contained in the plugin.xml file. This file is loaded into a registry when the platform starts up and so is always available, regardless of whether a plug-in has started. This allows the platform to present a plug-in's functionality to the user without going through the expense of loading and activating the code segment. Thus, a plug-in can contribute menus, actions, icons, editors, and so on, without ever being loaded. If the user tries to run an action or open a UI element associated with that plug-in, only then will the code for that plug-in be loaded.

To get down to specifics, a plug-in can be activated in three ways.

*   If a plug-in contributes an _executable extension_, another plug-in may run it, causing the plug-in to be automatically loaded. For more details, read the API javadoc for IExecutableExtension in the org.eclipse.core.runtime package.

*   If a plug-in exports one of its libraries (JAR files), another plug-in can reference and instantiate its classes directly. Loading a class belonging to a plug-in causes it to be started automatically.

*   Finally, a plug-in can be activated explicitly, using the API method Platform.getPlugin(). This method returns a fully initialized plug-in instance.

In all these cases, if the plug-in contributes a runtime plug-in object (subclassing org.eclipse.core.runtime.Plugin), its class initializer, constructor, and startup method will be run before any other class in the plug-in gets loaded. Of course, if the plug-in's constructor or startup method references any of its own classes, they will be loaded and, possibly, instantiated before the plug-in is fully initialized.

It is a common misconception that adding a plug-in to your requires list will cause it to be activated before your plug-in. This is _not true_. Your plug-in may very well be loaded and used without plug-ins in your requires list ever being started. Never assume that another plug-in has been started unless you know you have referenced one of its classes or executed one of its extensions.

To play along with the rule of lazy activation, plug-in writers should follow some general rules.

*   Do an absolute minimum of work in your Plugin.startup method. Does the code in your startup method need to be run immediately? Do you need to load those large in-memory structures right away? Consider deferring as much work as possible until it is needed.

*   Avoid referencing other plug-ins during your Plugin.startup. This can result in a sequence of cascading plug-in activations that ends up loading large amounts of unneeded code. Load other plug-ins-either through executable extensions or by referencing classes-only when you need them.

*   When defining extension points, make the extension _declarative_ as much as possible. Keep in mind that extensions can contribute text strings, icons, and simple logic statements via plugin.xml, allowing you to defer or possibly completely avoid plug-in activation.

See Also:
---------

*   [FAQ What is the plug-in manifest file (plugin.xml)?](./FAQ_What_is_the_plug-in_manifest_file_plugin_xml.md "FAQ What is the plug-in manifest file (plugin.xml)?")
*   [FAQ Can I activate my plug-in when the workbench starts?](./FAQ_Can_I_activate_my_plug-in_when_the_workbench_starts.md "FAQ Can I activate my plug-in when the workbench starts?")

