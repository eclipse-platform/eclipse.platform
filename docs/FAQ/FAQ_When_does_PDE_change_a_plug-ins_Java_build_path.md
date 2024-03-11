

FAQ When does PDE change a plug-in's Java build path?
=====================================================

Whenever you add dependent plug-ins in the Manifest Editor, the underlying classpath has to be updated to give you access to the classes defined by the plug-in. When you save the plugin.xml file, the editor will also update the Java build path: the .classpath file.

In Eclipse 3.0, the PDE uses a special classpath entry called a _classpath container_ to avoid having to directly modify the .classpath file every time your plug-in's dependencies change. During builds, this classpath container resolves to the set of dependencies specified in the plugin.xml file.

Regardless of whether classpath containers are used, it makes little sense to edit the .classpath file by hand or to make changes directly to the **Java Build Path** property page for plug-in projects. Any changes you make there will be silently overwritten the next time you save the Manifest Editor.

See Also:
---------

*   [FAQ Why doesn't my plug-in build correctly?](./FAQ_Why_doesnt_my_plug-in_build_correctly.md "FAQ Why doesn't my plug-in build correctly?")
*   [FAQ What is the classpath of a plug-in?](./FAQ_What_is_the_classpath_of_a_plug-in.md "FAQ What is the classpath of a plug-in?")

