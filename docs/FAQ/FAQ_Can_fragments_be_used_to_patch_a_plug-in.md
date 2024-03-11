

FAQ Can fragments be used to patch a plug-in?
=============================================

A common misconception is that a fragment can be used to patch or replace functionality in its host plug-in. Although this is possible to a certain extent, this is not what fragments were designed for. A plug-in and its fragments each contribute a manifest, and each may also contribute native libraries, Java code libraries, and other resources. At runtime, these contributions are all merged into a single manifest and a single namespace of libraries and resources. If a fragment defines the same library as its host, whether the fragment's library will be found over the host's library is undefined. This makes it impractical to use fragments as a way of replacing libraries or other resources defined by a plug-in.

Nonetheless, it is possible to design a plug-in so that it allows a portion of its functionality to be implemented or replaced by a fragment. Let's look at a notable example of how this is applied in the org.eclipse.swt plug-in. The SWT plug-in manifest declares a runtime library by using a special path-substitution variable:

      <library name="$ws$/swt.jar">

When the plug-in manifest is loaded, the platform will substitute the $ws$ variable with a string describing the windowing system of the currently running operating system. Each windowing system has a separate SWT plug-in fragment that will provide this library. For example, when running on windows, $ws$ will resolve to ws/win32. You can make use of this path-substitution facility in your own plug-in code by using the Plugin.find methods. The fragment org.eclipse.swt.win32 supplies the swt.jar library at the path org.eclipse.swt.win32/ws/win32/swt.jar. Thus, in this case the fragment will supply a library that was specified by its host plug-in.

The same principle can be used to allow a fragment to provide a patch to a host plug-in. The host plug-in can specify both its own library and a patch library in its plug-in manifest:

      <runtime>
         <library name="patch.jar">
            <export name="*"/>
         </library>
         <library name="main.jar">
            <export name="*"/>
         </library>
      </runtime>

  
The host plug-in puts all its code in main.jar and does not specify a patch.jar at all. When no patch is needed, the patch.jar library is simply missing from the classpath. This allows a fragment to be added later that contributes the patch.jar library. Because the host plug-in has defined patch.jar at the front of its runtime classpath, classes in the patch library will be found before classes in the original library.

This technique is used in Eclipse 3.0 to provide backward-compatibility support for plug-ins based on Eclipse 2.1 or earlier. The plug-in org.eclipse.ui.workbench defines a library called compatibility.jar at the start of its classpath. When the platform detects a plug-in written prior to Eclipse 3.0, a fragment called org.eclipse.ui.workbench.compatibility containing compatibility.jar is automatically added to the plug-in's classpath. This library adds back some old API that was moved in Eclipse 3.0. The beauty of this mechanism is that it allows the backward-compatibility support to be added or removed with no impact on the host plug-in.

See Also:
---------

[FAQ\_What\_is\_the\_classpath\_of\_a_plug-in?](./FAQ_What_is_the_classpath_of_a_plug-in.md "FAQ What is the classpath of a plug-in?")


