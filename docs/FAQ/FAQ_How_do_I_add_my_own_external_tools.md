

FAQ How do I add my own external tools?
=======================================

  

External tools are applications or scripts that typically act as extensions to your development environment. For example, they may be used to execute scripts to package and deploy your application or to run an external compiler on your source files. External tools allow an end user to achieve a basic level of integration for a non-Eclipse-aware tool without writing a plug-in. External tools are created and configured via **Run > External Tools > External Tools** or from the drop-down menu on the **Run** button with the toolbox overlay.

If you want to write your own category of external tool, such as support for a different scripting language, you need to write a plug-in. The process for defining external tools is almost identical to writing your own launch configuration. Essentially, an external tool is a launch-configuration type that belongs to the special external-tools category:

    <launchConfigurationType
      name="My Tool"
      delegate="com.xyz.MyLaunchDelegate"
      **category="org.eclipse.ui.externaltools"**
      modes="run"
      id="com.xyz.MyLaunchType">
    </launchConfigurationType>

  

See Also:
---------

[FAQ What is a launch configuration?](./FAQ_What_is_a_launch_configuration.md "FAQ What is a launch configuration?")

