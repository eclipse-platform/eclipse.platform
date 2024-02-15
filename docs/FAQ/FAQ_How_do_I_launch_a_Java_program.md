

FAQ How do I launch a Java program?
===================================

JDT has support for launching Java programs. First, add the following plug-ins to your dependent list:


*   org.eclipse.debug.core

*   org.eclipse.jdt.core

*   org.eclipse.jdt.launching

With those plug-ins added to your dependent plug-in list, your Java program can be launched using the JDT in two ways. In the first approach, an IVMRunner uses the currently installed VM, sets up its classpath, and asks the VM runner to run the program:

      void launch(IJavaProject proj, String main) {
         IVMInstall vm = JavaRuntime.getVMInstall(proj);
         if (vm == null) vm = JavaRuntime.getDefaultVMInstall();
         IVMRunner vmr = vm.getVMRunner(ILaunchManager.RUN_MODE);
         String[] cp = JavaRuntime.
            computeDefaultRuntimeClassPath(proj);
         VMRunnerConfiguration config = 
            new VMRunnerConfiguration(main, cp);
         ILaunch launch = new Launch(null, 
            ILaunchManager.RUN_MODE, null);
         vmr.run(config, launch, null);
      }

  
The second approach is to create a new launch configuration, save it, and run it. The cfg parameter to this method is the name of the launch configuration to use:

      void launch(IJavaProject proj, String cfg, String main) {
         DebugPlugin plugin = DebugPlugin.getDefault();
         ILaunchManager lm = plugin.getLaunchManager();
         ILaunchConfigurationType t = lm.getLaunchConfigurationType(
         IJavaLaunchConfigurationConstants.ID\_JAVA\_APPLICATION);
         ILaunchConfigurationWorkingCopy wc = t.newInstance(
         null, cfg);
         wc.setAttribute(
         IJavaLaunchConfigurationConstants.ATTR\_PROJECT\_NAME, 
         proj.getElementName());
         wc.setAttribute(
         IJavaLaunchConfigurationConstants.ATTR\_MAIN\_TYPE_NAME, 
         main);
         ILaunchConfiguration config = wc.doSave();   
         config.launch(ILaunchManager.RUN_MODE, null);
      }

More information is available at **Help > Help Contents > JDT Plug-in Developer Guide** \> JDT Debug > Running Java code**.**

  

See Also:
---------

[FAQ What is a launch configuration?](./FAQ_What_is_a_launch_configuration.md "FAQ What is a launch configuration?")

