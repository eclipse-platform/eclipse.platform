

FAQ What is the classpath of a plug-in?
=======================================

Developers coming from a more traditional Java programming environment are often confused by classpath issues in Eclipse. A typical Java application has a global namespace made up of the contents of the JARs on a single universal classpath. This classpath is typically specified either with a command line argument to the VM or by an operating system environment variable. In Eclipse, each plug-in has its own unique classpath. This classpath contains the following, in lookup order:

*   _The OSGi parent class loader_. All class loaders in OSGi have a common parent class loader. By default, this is set to be the Java boot class loader. The boot loader typically only knows about rt.jar, but the boot classpath can be augmented with a command line argument to the VM.

*   _The exported libraries of all imported plug-ins_. If imported plug-ins export their imports, you get access to their exported libraries, too. Plug-in libraries, imports, and exports are all specified in the plugin.xml file.

*   _The declared libraries of the plug-in and all its fragments_. Libraries are searched in the order they are specified in the manifest. Fragment libraries are added to the end of the classpath in an unspecified order.

In Eclipse 2.1, the libraries from the org.eclipse.core.boot and org.eclipse.core.runtime were also automatically added to every plug-in's classpath. This is not true in 3.0; you now need to declare the runtime plug-in in your manifest's requires section, as with any other plug-in.

  

See Also:
---------

*   [FAQ What is the plug-in manifest file (plugin.xml)?](./FAQ_What_is_the_plug-in_manifest_file_plugin_xml.md "FAQ What is the plug-in manifest file (plugin.xml)?")
*   [FAQ How do I make my plug-in connect to other plug-ins?](./FAQ_How_do_I_make_my_plug-in_connect_to_other_plug-ins.md "FAQ How do I make my plug-in connect to other plug-ins?")
*   [FAQ How do I add a library to the classpath of a plug-in?](./FAQ_How_do_I_add_a_library_to_the_classpath_of_a_plug-in.md "FAQ How do I add a library to the classpath of a plug-in?")
*   [FAQ How can I share a JAR among various plug-ins?](./FAQ_How_can_I_share_a_JAR_among_various_plug-ins.md "FAQ How can I share a JAR among various plug-ins?")
*   [FAQ How do I use the context class loader in Eclipse?](./FAQ_How_do_I_use_the_context_class_loader_in_Eclipse.md "FAQ How do I use the context class loader in Eclipse?")
*   [Pragmatic Advice on PDE Classpath](/Pragmatic_Advice_on_PDE_Classpath "Pragmatic Advice on PDE Classpath")

