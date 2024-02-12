

FAQ How do I create an external tool builder?
=============================================

An external tool builder is an external tool that runs every time the projects in your workspace are built. End users can add external tool builders by selecting a project in the Navigator, choosing **Properties**, and then going to the **Builders** page. On the **Build Options** tab, you can specify whether the builder should run on autobuild, manual build, or on **Clean**. In most cases, running external tool builders during auto-builds is too disruptive because they are too long running.

As with ordinary external tools, you can define your own type of external tool builder by creating a new launch configuration type. In your launch configuration declaration, you must specify the category for external tool builder launch configurations:

      <launchConfigurationType
         name="%AntBuild"
         delegate="com.xyz.MyLaunchDelegate"
         '''category="org.eclipse.ui.externaltools.builder"'''
         modes="run"
         id="com.xyz.MyLaunchType">
      </launchConfigurationType>

See Also:
---------

[FAQ\_What\_is\_a\_launch_configuration?](./FAQ_What_is_a_launch_configuration.md "FAQ What is a launch configuration?")

[FAQ\_How\_do\_I\_add\_my\_own\_external\_tools?](./FAQ_How_do_I_add_my_own_external_tools.md "FAQ How do I add my own external tools?")

