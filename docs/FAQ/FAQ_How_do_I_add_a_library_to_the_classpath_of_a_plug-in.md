

FAQ How do I add a library to the classpath of a plug-in?
=========================================================

In[FAQ\_What\_is\_the\_classpath\_of\_a_plug-in?](./FAQ_What_is_the_classpath_of_a_plug-in.md "FAQ What is the classpath of a plug-in?") we explained how the classpath for a plug-in is computed. To access a given library from a plug-in, the library needs to be added to the classpath of the plug-in.

A JAR can be added to the classpath of a plug-in in four ways.

*   The JAR can be added to the boot classpath. This is generally a

bad idea, however, as it requires an extra VM argument, and it also affects the classpath of all other installed plug-ins. If the JAR adds types-classes or interfaces-that mask types declared in other plug-ins, you will probably break those other plug-ins. Nonetheless, if you are looking for a quick and dirty hack, this is the easiest approach.

*   The JAR can be added to the declared libraries for a plug-in. This is fine

if you don't anticipate a need for other plug-ins also to use that JAR.

*   A new plug-in can be created that is a wrapper for the

library; then the new plug-in is added to the list of required plug-ins for all plug-ins that want access to the library.

*   The OSGi parent loader can be changed by

setting the osgi.parentClassloader system property on startup. This is also generally a bad idea, for the same reasons listed for changing the boot classpath. Valid values for the parent loader property are:

*   boot. The Java boot class loader. This is the default OSGi

parent loader, and has access to all JARs on the VM's boot classpath.

*   ext. The Java extension class loader. This class loader has

access to the JARs placed in the ext directory in the JVM's install directory. The parent of the extension loader is typically the boot class loader.

*   app. The Java application class loader. This class loader has access

to the traditional classpath entries specified by the -classpath command line argument. In Eclipse this typically includes only the bootstrap classes in startup.jar. The parent of the application class loader is the extension class loader.

*   fwk. The OSGi framework class loader. This is the class loader

that is responsible for starting the OSGi framework. Typically you will not want to use the class loader, as its classpath is not strictly specified.
  
Using a separate plug-in to contain a library is the most powerful approach because it means that other plug-ins can make use of that library without having to load your plug-in or add the library to their own classpath explicitly. This approach is used throughout the Eclipse Project to add third-party libraries, such as Xerces, Ant, and JUnit.

Of course, because this is Java, there is always a way to load classes outside the scope of your classpath. You can instantiate your own class loader that knows how to find the code you need and use that to load other classes. This is a very powerful mechanism because it can change dynamically at runtime, and it can even load classes that aren't in your file system, such as classes in a database or even classes generated on the fly. Manipulating class loaders is a bit outside the scope of this book, but plenty of information is available in Java programming books or at the [Java Web site](http://java.sun.com).

See Also:
---------

[FAQ\_How\_can\_I\_share\_a\_JAR\_among\_various_plug-ins?](./FAQ_How_can_I_share_a_JAR_among_various_plug-ins.md "FAQ How can I share a JAR among various plug-ins?")

  
[FAQ\_What\_is\_a\_plug-in_fragment?](./FAQ_What_is_a_plug-in_fragment.md "FAQ What is a plug-in fragment?")

