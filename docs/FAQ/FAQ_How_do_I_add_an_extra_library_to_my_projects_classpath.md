

FAQ How do I add an extra library to my project's classpath?
============================================================

Open the context menu on the project, and select **Properties > Java Build Path > Libraries**. From here, you can add JAR files to the build path, whether they are inside your workspace or not. You can also add a _class folder_, a directory containing Java class files that are not in a JAR. Note that plug-in development has more restrictions on where build path entries can come from. For plug-in projects, you should always let the plug-in development tools organize your build path for you. Simply select the plugin.xml file, and choose **PDE Tools > Update Classpath** from the context menu.

See Also:
---------

*   [FAQ How do I set up a Java project to share in a repository?](./FAQ_How_do_I_set_up_a_Java_project_to_share_in_a_repository.md "FAQ How do I set up a Java project to share in a repository?")
*   [FAQ When does PDE change a plug-in's Java build path?](./FAQ_When_does_PDE_change_a_plug-ins_Java_build_path.md "FAQ When does PDE change a plug-in's Java build path?")
*   [FAQ What is the classpath of a plug-in?](./FAQ_What_is_the_classpath_of_a_plug-in.md "FAQ What is the classpath of a plug-in?")

