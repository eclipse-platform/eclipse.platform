

FAQ What are extensions and extension points?
=============================================

A basic rule for building modular software systems is to avoid tight coupling between components. If components are tightly integrated, it becomes difficult to assemble the pieces into different configurations or to replace a component with a different implementation without causing a ripple of changes across the system.

Loose coupling in Eclipse is achieved partially through the mechanism of extensions and extension points. The simplest metaphor for describing extensions and extension points is electrical outlets. The outlet, or socket, is the extension point; the plug, or light bulb that connects to it, the extension. As with electric outlets, extension points come in a wide variety of shapes and sizes, and only the extensions that are designed for that particular extension point will fit.

When a plug-in wants to allow other plug-ins to extend or customize portions of its functionality, it will declare an extension point. The extension point declares a contract, typically a combination of XML markup and Java interfaces, that extensions must conform to. Plug-ins that want to connect to that extension point must implement that contract in their extension. The key attribute is that the plug-in being extended knows nothing about the plug-in that is connecting to it beyond the scope of that extension point contract. This allows plug-ins built by different individuals or companies to interact seamlessly, even without their knowing much about one another.

The Eclipse Platform has many applications of the extension and extension point concept. Some extensions are entirely _declarative_; that is, they contribute no code at all. For example, one extension point provides customized key bindings, and another defines custom file annotations, called _markers_; neither of these extension points requires any code on behalf of the extension.

Another category of extension points is for overriding the default behavior of a component. For example, the Java development tools include a code formatter but also supply an extension point for third-party code formatters to be plugged in. The resources plug-in has an extension point that allows certain plug-ins to replace the implementation of basic file operations, such as moving and deletion.

Yet another category of extension points is used to group related elements in the user interface. For example, extension points for providing views, editors, and wizards to the UI allow the base UI plug-in to group common features, such as putting all import wizards into a single dialog, and to define a consistent way of presenting UI contributions from a wide variety of other plug-ins.

See Also:
---------

*   [FAQ How do I declare my own extension point?](./FAQ_How_do_I_declare_my_own_extension_point.md "FAQ How do I declare my own extension point?")
*   [FAQ What is a plug-in?](./FAQ_What_is_a_plug-in.md "FAQ What is a plug-in?")
*   [FAQ What is the plug-in manifest file (plugin.xml)?](./FAQ_What_is_the_plug-in_manifest_file_plugin_xml.md "FAQ What is the plug-in manifest file (plugin.xml)?")
*   [FAQ How do I make my plug-in connect to other plug-ins?](./FAQ_How_do_I_make_my_plug-in_connect_to_other_plug-ins.md "FAQ How do I make my plug-in connect to other plug-ins?")
*   [FAQ What is an extension point schema?](./FAQ_What_is_an_extension_point_schema.md "FAQ What is an extension point schema?")
*   _Platform Plug-in Developer Guide_ under Programmer's Guide, Platform architecture

