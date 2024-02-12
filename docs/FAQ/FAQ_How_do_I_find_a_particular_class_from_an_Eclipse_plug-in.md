

FAQ How do I find a particular class from an Eclipse plug-in?
=============================================================

Suppose that you ask a question on the Eclipse newsgroup, and someone answers, "That's easy; just look at JavaAnnotationImageProvider". You are left scratching your head, wondering how to find that particular needle in the Eclipse haystack. Once you are familiar with searching for things in Eclipse, you too will come to realize that package names are rarely needed when browsing for information in the Eclipse source.

The first thing you need to do is set up your workspace so that all Eclipse plug-ins are found by the Java search engine. This can be accomplished by loading all the Eclipse plug-ins into your workspace, but this quickly results in a cluttered workspace in which it is difficult to find your own projects. There are two easier approaches to adding Eclipse plug-ins to the Java search engine's index.

**Option 1**

In Eclipse 3.5 (Galileo) or later

*   Open the Plug-in Development Preference Page by going to **Window > Preferences > Plug-in Development**.
*   Check the box marked **Include all plug-ins from target in Java search**.

**Option 2**

*   Activate the 'Plug-ins' view by going to **Window > Show View > Other > PDE > Plug-ins**.
*   Select all plug-ins in the view.
*   From the context menu, select **Add to Java Search**.

Once you have done this, switch back to the Java perspective and use **Navigate > Open Type** (or press Ctrl + Shift + T) and start typing the name of the class or interface you are looking for. You will now be able to quickly open an editor on any Java type in the Eclipse Platform. If you are searching for the plug-in that contains that class, you will find that information in the bottom of the 'Open Type' dialog. Please note that a class that's in package x.y.z is not guaranteed to be in plug-in x.y.z, it may be contributed by another plug-in.

In case you are curious, this works by creating in your workspace a Java project called External Plug-in Libraries. This project will have all the Eclipse plug-ins you selected on its build path, which ensures that they will be consulted by the Java search engine when searching for and opening Java types. You can use a similar technique to add other Java libraries to the search index. Simply add the JARs you want to be able to search to the build path of any Java project in your workspace, and they will automatically be included in the search.

See Also:
---------

*   [FAQ How do I open a type in a Java editor?](./FAQ_How_do_I_open_a_type_in_a_Java_editor.md "FAQ How do I open a type in a Java editor?")
*   [FAQ Where can I find the Eclipse plug-ins?](./FAQ_Where_can_I_find_the_Eclipse_plug-ins.md "FAQ Where can I find the Eclipse plug-ins?")

Note:
-----

As of Eclipse 3.4 (Ganymede) the External Plug-in Libraries Java project is hidden by default (see [194694](https://bugs.eclipse.org/bugs/show_bug.cgi?id=194694)). Use the Java element filters dialog to include the External Plug-in Libraries project in the Package Explorer view.

