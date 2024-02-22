

FAQ What is the use of the build.xml file?
==========================================

The build.xml file is an Ant script that is created by the PDE to take your plug-in components and combine them into a deployable format. This file compiles and archives your plug-in source code into a single JAR file. The build.properties file controls what goes into your plug-in distribution.

The build.xml file can be created by using the context menu on plugin.xml and selecting **PDE Tools > Create Ant Build File**.

Ant build files are low-level mechanisms to package up plug-ins. A much easier way to share and deploy plug-ins is to create a feature for your plug-in and then an update site. The Update Site Editor has a very handy button, **Build All...**, that will create the build.xml behind the scenes, run it, and collect all the output without cluttering your plug-in and feature projects.

See Also:
---------

*   [FAQ When is the build.xml script executed?](./FAQ_When_is_the_build_xml_script_executed.md "FAQ When is the build.xml script executed?")
*   [FAQ How do I create a feature?](./FAQ_How_do_I_create_a_feature.md "FAQ How do I create a feature?")
*   [FAQ How do I create an update site?](./FAQ_How_do_I_create_an_update_site.md "FAQ How do I create an update site")
*   [FAQ What is Ant?](./FAQ_What_is_Ant.md "FAQ What is Ant?")

