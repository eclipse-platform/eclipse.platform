

FAQ Why don't my markers appear in the editor's vertical ruler?
===============================================================

Text editors in Eclipse can display markers in a number of ways. Most commonly, they appear as icons in the vertical ruler on the left-hand side of the editor pane. Markers can also optionally be displayed as squiggly underlines in the text and in the overview ruler on the right-hand side of the editor. How each type of marker is displayed is chosen by the user on the editor preference pages (**Workbench > Editors > Text Editor > Annotations** and **Java > Editor > Annotations**). The IMarker interface declares a number of frequently used marker types. Any created marker that has either the LINE_NUMBER or CHAR_START and CHAR_END attributes set will be displayed by editors. These attributes must exist when the marker is created for the marker to appear in an editor. The most common mistake is to create the marker and then add the attributes in a separate operation. The text framework provides a utility class called MarkerUtilities to make this easier for you. Here is a sample snippet that adds a marker correctly:

      int lineNumber = ...;
      HashMap map = new HashMap();
      MarkerUtilities.setLineNumber(map, lineNumber);
      MarkerUtilities.createMarker(resource, map, IMarker.TEXT);

  

See Also:
---------

[FAQ How do I create my own tasks, problems, bookmarks, and so on?](./FAQ_How_do_I_create_my_own_tasks_problems_bookmarks_and_so_on.md "FAQ How do I create my own tasks, problems, bookmarks, and so on?")

