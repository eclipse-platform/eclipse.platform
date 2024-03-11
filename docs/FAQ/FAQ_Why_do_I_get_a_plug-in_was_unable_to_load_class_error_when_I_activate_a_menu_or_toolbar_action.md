

FAQ Why do I get a 'plug-in was unable to load class' error when I activate a menu or toolbar action?
=====================================================================================================

It is possible to get the following error message on the console:

   Could not create action delegate for id: sample.Action
   Reason:
      Plug-in sample was unable to load class sample.Action.

The most likely reason is that an exception was thrown in the static initializer for a class declared by the offending plug-in. Check the .log file to see whether that indeed happened. The Eclipse Platform loader will not load a plug-in when exceptions are thrown during the initialization of the Java classes that make up the plug-in. Another common reason for this error is the lack of an appropriate constructor for the class being loaded. Most classes declared in extension points must have a public zero-argument constructor. Check the extension point documentation to see what constructor is required for the classes that you declare in an extension.

If the problem only occurs when deploying a packaged plug-in (i.e., when it is not started in a runtime workbench via PDE) it is usually a good idea to check the `Bundle-ClassPath` attribute in the `MANIFEST.MF` file. The JAR file that contains the plug-in classes must be listed in the `Bundle-ClassPath`. Even if the plug-in's proper classes are all listed, class loading may still fail because a `.class` file may contain references to other classes that cannot be resolved at runtime. In this case, the missing classes need to be identified (usually by looking at the `import` statements of the problematic class) and the necessary entries need to be added to the `Bundle-ClassPath`. If additional JAR files are required, those JARs also need to be listed in the `build.properties` file so that they are included when the plug-in is packaged.

