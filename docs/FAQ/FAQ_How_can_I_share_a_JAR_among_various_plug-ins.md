

FAQ How can I share a JAR among various plug-ins?
=================================================

Suppose that plug-in _A_ and plug-in _B_ both use xmlparser.jar. In your workspace are two projects (for _A_ and _B_), each containing a copy of the xmlparser.jar library. This is clearly not ideal: Two copies of the JAR are loaded at runtime, and classes from those JARs will not be compatible with each other, as they are loaded by different class loaders. (You will get a ClassCastException if you try to cast a type from one library into a type from the other library.)

  

Declaring xmlparser.jar as an external JAR does not work, as there is no easy way during deployment of your plug-ins to manipulate your plug-in's classpath so that they can see the library. The best way to share libraries is to create a new plug-in that wraps the library you want to share.

  

Declare a new plug-in, _C_, to contain the library JAR, and make both plug-in _A_ and plug-in _B_ dependent on plug-in _C_. Make sure that plug-in _C_ exports its library so other plug-ins can see it:

      <runtime>
         <library name="xmlParserAPIs.jar">
            <export name="*"/>
         </library>
      </runtime>

  

When you deploy these three plug-ins, they will all share the same library. Note that in some situations, sharing libraries between plug-ins is not possible. If two plug-ins require different or incompatible versions of the same library, they have no choice but to each have a copy of the library.

  

  

  

See Also:
---------

[FAQ\_What\_is\_the\_classpath\_of\_a_plug-in?](./FAQ_What_is_the_classpath_of_a_plug-in.md "FAQ What is the classpath of a plug-in?")

  
[FAQ\_How\_do\_I\_add\_a\_library\_to\_the\_classpath\_of\_a\_plug-in?](./FAQ_How_do_I_add_a_library_to_the_classpath_of_a_plug-in.md "FAQ How do I add a library to the classpath of a plug-in?")

