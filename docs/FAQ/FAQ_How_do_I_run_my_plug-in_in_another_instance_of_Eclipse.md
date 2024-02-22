

FAQ How do I run my plug-in in another instance of Eclipse?
===========================================================

You can easily run a plug-ins in another instance of Eclipse by selecting **Run > Run As > Run-time Workbench**. This will launch a new workbench on the same Java runtime as your development workbench, with all plug-ins in your current workspace enabled, and starting in a special runtime workspace.

  

Another way of running your plug-in for the first time is by selecting the **Overview** tab of the plug-in Manifest Editor and clicking the link to **Run-time Workbench**.

  

After you close the runtime workbench, select **Run...** to edit the launch configuration for your runtime workbench. In the Launch Configuration Editor, you can select a logical name for this plug-in test scenario, which is useful when you want to experiment with multiple scenarios.

  

The Launch Configuration Editor also lets you choose which plug-ins to enable from your workspace and, in the case of conflicts with already installed plug-ins, which one to choose. Furthermore, you can choose which JRE to launch with, if you want or need to experiment with different VMs.

  
Finally, the editor allows you to specify special arguments, such as commands to increase the Java heap size available or to enable a special profiling library, to the VM.

  

Once you have launched a runtime workbench, pressing Ctrl+F11 will run it again, and F11 will launch the Eclipse debugger to debug your runtime workbench.

  

  

See Also:
---------

[FAQ\_What\_is\_a\_launch_configuration?](./FAQ_What_is_a_launch_configuration.md "FAQ What is a launch configuration?")

