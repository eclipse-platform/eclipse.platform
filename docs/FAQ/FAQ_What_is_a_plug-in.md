

FAQ What is a plug-in?
======================

In retrospect, _plug-in_, perhaps wasn't the most appropriate term for the components that build up an Eclipse application. The term implies the existence of a socket, a monolithic machine or grid that is being plugged into. In Eclipse, this isn't the case. A plug-in connects with a universe of other plug-ins to form a running application. The best software analogy compares a plug-in to an object in object-oriented programming. A plug-in, like an object, is an encapsulation of behavior and/or data that interacts with other plug-ins to form a running program.

A better question in the context of Eclipse is, What isn't a plug-in? A single Java source file, Main.java, is not part of a plug-in. This class is used only to find and invoke the plug-in responsible for starting up the Eclipse Platform. This class will typically in turn be invoked by a native executable, such as eclipse.exe on Windows, although this is just icing to hide the incantations required to find and launch a Java virtual machine. In short, just about everything in Eclipse is a plug-in.

More concretely, a plug-in minimally consists of a _bundle manifest file_, MANIFEST.MF. This manifest provides important details about the plug-in, such as its name, ID, and version number. The manifest may also tell the platform what Java code it supplies and what other plug-ins it requires, if any. Note that everything except the basic plug-in description is optional. A plug-in may provide code, or it may provide only documentation, resource bundles, or other data to be used by other plug-ins. A plug-in also typically provides a _plug-in manifest file_, plugin.xml, that describes how it extends other plug-ins, or what capabilities it exposes to be extended by others (extensions and extension points).

A plug-in that provides Java code may specify in the manifest a concrete subclass of org.eclipse.core.runtime.Plugin. This class consists mostly of convenience methods for accessing various platform utilities, and it may also implement startup and shutdown methods that define the lifecycle of the plug-in within the platform.

See Also:
---------

*   [FAQ What is the plug-in manifest file (plugin.xml)?](./FAQ_What_is_the_plug-in_manifest_file_plugin_xml.md "FAQ What is the plug-in manifest file (plugin.xml)?")
*   [FAQ What are extensions and extension points?](./FAQ_What_are_extensions_and_extension_points.md "FAQ What are extensions and extension points?")

