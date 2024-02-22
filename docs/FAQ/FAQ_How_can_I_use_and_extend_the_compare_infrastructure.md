

FAQ How can I use and extend the compare infrastructure?
========================================================

The org.eclipse.compare plug-in includes infrastructure for computing the differences between groups of files and the content of individual files. The plug-in also contains UI components that allow a user to browse and manipulate comparisons in editors and dialogs. Eclipse components typically interact with the compare plug-in in two ways. First, they may be compare clients, using the comparison engine to compute differences between various inputs and to display the result of those comparisons. For example, the Team plug-ins use the compare support to compute differences between local and remote resources, and to display those comparisons in a comparison editor.

  
The second way to use the compare plug-in is as a provider. Your plug-in can contribute viewers for displaying the structure of files and for illustrating the differences between two files of a particular type. _Structure merge viewers_ are contributed when you have a file with a particular semantic structure that is useful to display to the user. For example, JDT supplies a structure merge viewer that shows the differences between the methods and fields of the files being displayed. Structure viewers are contributed by using the structureMergeViewers extension point.

  
The platform includes compare viewers for text content and for GIF and JPG image files. If you have unique files that need a more specialized compare viewer, you can contribute one via an extension point. All plug-ins that use compare will then be able to make use of those compare viewers for displaying your content type. To contribute a content viewer that does not support merge, use the contentViewers extension point. To contribute a viewer that supports the merging of files, use the contentMergeViewers extension point.

  

See Also:
---------

Go to **Platform Plug-in Developer Guide > Programmer's Guide >** Compare support**.**

