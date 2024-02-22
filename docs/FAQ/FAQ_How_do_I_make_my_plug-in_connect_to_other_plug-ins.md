

FAQ How do I make my plug-in connect to other plug-ins?
=======================================================

Like members of a community, plug-ins do not generally live in isolation. Most plug-ins make use of services provided by other plug-ins, and many, in turn, offer services that other plug-ins can consume. Some groups of plug-ins are tightly related, such as the group of plug-ins providing Java development tools-the JDT plug-ins-and other plug-ins, such as SWT, stand alone without any awareness of the plug-ins around them. Plug-ins can also expose a means for other plug-ins to customize the functionality they offer, just as a handheld drill has an opening that allows you to insert other attachments such as screwdrivers and sanders. When designing a plug-in, you need to think about what specific plug-ins or services it will need, what it will expose to others, and in what ways it wants to allow itself to be customized by others.

  

  
To rephrase all this in Eclipse terminology, plug-ins define their interactions with other plug-ins in a number of ways. First, a plug-in can specify what other plug-ins it _requires_, those that it absolutely cannot live without. A UI plug-in will probably require the SWT plug-in, and a Java development tool will usually require one or more of the JDT plug-ins. Plug-in requirements are specified in the _plug-in manifest file_ (plugin.xml). The following example shows a plug-in that requires only the JFace and SWT plug-ins:

    <requires>
        <import plugin="org.eclipse.jface"/>
        <import plugin="org.eclipse.swt"/>
    </requires>

Your plug-in can reference _only_ the classes and interfaces of plug-ins it requires. Attempts to reference classes in other plug-ins will fail.

  

  
Conversely, a plug-in can choose which classes and interfaces it wants to expose to other plug-ins. Your plug-in manifest must declare what libraries (JARs) it provides and, optionally, what classes it wants other plug-ins to be able to reference. This example declares a single JAR file and exposes classes only in packages starting with the prefix com.xyz.*:

    <runtime>
      <library name="sample.jar">
        <export name="com.xyz.*"/>
      </library>   
    </runtime>

  

  
Finally, a plug-in manifest can specify ways that it can be customized (_extension points_) and ways that it customizes the behavior of other plug-ins (_extensions_).

  

  

  

See Also:
---------

\[\[FAQ\_What\_is\_the\_plug-in\_manifest\_file_%28%3Ctt%3Eplugin.xml%3C%2Ftt%3E%29%3F\]\]

  
[FAQ\_What\_are\_extensions\_and\_extension\_points?](./FAQ_What_are_extensions_and_extension_points.md "FAQ What are extensions and extension points?")

  
[FAQ\_What\_is\_the\_classpath\_of\_a_plug-in?](./FAQ_What_is_the_classpath_of_a_plug-in.md "FAQ What is the classpath of a plug-in?")

