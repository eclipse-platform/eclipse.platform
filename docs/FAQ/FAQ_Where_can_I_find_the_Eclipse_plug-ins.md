

FAQ Where can I find the Eclipse plug-ins?
==========================================

The easiest way to get plug-ins from Eclipse into your workspace is by invoking **File > Import > External Plug-ins and Fragments** and selecting the ones you are interested in loading. You can choose either to load all of the source, allowing you to hack on it, or to import binary plug-ins for the purpose of browsing and searching.

You can also load the latest Eclipse source from the Eclipse CVS repository. The access info is

    :pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse

In other words, go to the CVS Repository Exploring perspective and add a new repository location with host dev.eclipse.org, repository path /home/eclipse, and user name anonymous; a password is not required. Once you see the repository location in the CVS Repositories view, browse it by going into HEAD and select the project you are interested in. Use the context menu on a project to check it out into your current workspace.

Loading plug-ins into your workspace can result in build path problems for both newly loaded plug-ins and existing plug-ins in your workspace. When the plug-in you just checked out relies on other plug-ins that are not yet in your workspace, you are bound to get many complaints, such as those in Figure 4.3.

To fix references to missing plug-ins, either add the missing plug-ins to your workspace or refer to plug-ins in your base Eclipse installation. The latter is much easier; simply run **PDE Tools > Update Classpath...** from the project's context menu. However, remember that when checking out from CVS, you are obtaining bleeding-edge contents that committers may have modified minutes earlier. If you mix plug-ins from different builds of Eclipse, such as some from CVS and some from your Eclipse install, incompatibilities may be insurmountable. Your best bet is to make sure that all plug-ins in your workspace are from the same Eclipse build.

It may be a struggle to keep your workspace error free when you bring in only portions of the Eclipse Platform. Eclipse can become just as confused as you. When all else fails, run **Update Classpath...** on all plug-ins in your workspace. Running **Project > Clean...** to completely discard all build states and force a fresh build may also help.

See Also:
---------

*   [FAQ When does PDE change a plug-in's Java build path?](./FAQ_When_does_PDE_change_a_plug-ins_Java_build_path.md "FAQ When does PDE change a plug-in's Java build path?")

