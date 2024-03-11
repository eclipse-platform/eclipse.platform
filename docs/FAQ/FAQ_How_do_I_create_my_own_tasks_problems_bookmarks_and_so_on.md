

FAQ How do I create my own tasks, problems, bookmarks, and so on?
=================================================================

Annotations can be added to resources in the workspace by creating IMarker objects. These markers are used to represent compile errors, to-do items, bookmarks, search results, and many other types of annotations. You can create your own marker types for storing annotations for use by your own plug-in. Each marker can store an arbitrary set of attributes. Attributes are keyed by a string, and the values can be strings, Booleans, or integers. The IMarker interface defines some common attribute types, but you are free to create your own attribute names for your markers. Here is an example snippet that creates a marker, adds some attributes, and then deletes it:

            final IFile file = null;
            IMarker marker = file.createMarker(IMarker.MARKER);
            marker.setAttribute(IMarker.MESSAGE, "This is my marker");
            marker.setAttribute("Age", 5);
            marker.delete();

When markers are created, modified, or deleted, a resource change event will be broadcast, telling interested parties about the change. You can search for markers by using the findMarkers methods on IResource.

The org.eclipse.core.resources.markers extension point can be used to declaratively define new marker types. See its documentation for an explanation and examples.

See Also:
---------

*   [FAQ How can I be notified of changes to the workspace?](./FAQ_How_can_I_be_notified_of_changes_to_the_workspace.md "FAQ How can I be notified of changes to the workspace?")
*   [FAQ Why don't my markers appear in the editor's vertical ruler?](./
FAQ_Why_dont_my_markers_appear_in_the_editors_vertical_ruler.md  "FAQ Why don't my markers appear in the editor's vertical ruler?")
*   [FAQ How do I create problem markers for my compiler?](./FAQ_How_do_I_create_problem_markers_for_my_compiler.md "FAQ How do I create problem markers for my compiler?")
*   Go to **Platform Plug-in Developer Guide > Programmer's Guide > Resources overview > Resource markers**
*   Eclipse online article ["Mark My Words"](https://www.eclipse.org/articles/Article-Mark%20My%20Words/mark-my-words.html)

