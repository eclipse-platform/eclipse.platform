

FAQ Why doesn't my plug-in build correctly?
===========================================

It is important to realize that a plug-in consists of a manifest description written in XML and an independent implementation written in Java. The plugin.xml file defines the prerequisite plug-ins and effectively defines the classpath for your own plug-in classes. A typical build problem is caused by a change to the build classpath, often indirectly owing to a change to the plugin.xml file. This may happen when you extract a plug-in out of CVS, for instance. The classpath settings are copied from the CVS repository but were put there by someone who may have had a different installation location for Eclipse. Be careful, as the classpath consists mainly of hard-coded file system locations.

To recompute the classpath, use the context menu on your project: **PDE Tools > Update Classpath**. This will instruct PDE to look at your plugin.xml file and construct a build classpath specific to your Eclipse installation and workspace contents.

If your plug-in relies on other broken plug-ins in your workspace, your plug-in may not be able to build itself. Start with the offending plug-in and work your way up the dependency hierarchy to find the problem. When all else fails, try **Project >Clean...** to force everything to be rebuilt from scratch.

See Also:
---------

*   [FAQ How do I set up a Java project to share in a repository?](./FAQ_How_do_I_set_up_a_Java_project_to_share_in_a_repository.md "FAQ How do I set up a Java project to share in a repository?")
*   [FAQ What is the classpath of a plug-in?](./FAQ_What_is_the_classpath_of_a_plug-in.md "FAQ What is the classpath of a plug-in?")

