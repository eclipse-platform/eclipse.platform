

FAQ Why is the interface for my new extension point not visible?
================================================================

When you declare a new extension point and a corresponding interface to implement, plug-ins that contribute to your extension point sometimes cannot see your interface. The reason is that your interface may not match the export tag' regular expression in your plug-in's runtime library tag. If your interface is called com.xyz.MyInterface, your plugin.xml should look like this:

      <runtime>
         <library> name="sample.jar">
            <export name="com.xyz.MyInterface"/>
         <library>
      <runtime>

Multiple export tags can be used, in addition to wildcards. The default tag set by the PDE is *, indicating that all types in the JAR should be exported.

