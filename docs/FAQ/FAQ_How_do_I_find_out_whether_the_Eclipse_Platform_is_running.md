

FAQ How do I find out whether the Eclipse Platform is running?
==============================================================

If you have a library of code that can be used within both the Eclipse Platform and a stand-alone application, you may need to find out programmatically whether the Eclipse Platform is running. In Eclipse 3.0, this is accomplished by calling Platform.isRunning. In 2.1, call BootLoader.isRunning. You will need to set up the classpath of your stand-alone application to make sure that the boot or runtime plug-in's library is reachable. Alternatively, you can reference the necessary class via reflection.

  

  
You can find out whether an Ant script is running from within Eclipse by querying the state of the variable eclipse.running. You can use this information to specify targets that are built only when Ant is invoked from within Eclipse:

      <target name="properties" if="eclipse.running"/>

  

  

  

See Also:
---------

[FAQ What is Ant?](./FAQ_What_is_Ant.md "FAQ What is Ant?")

