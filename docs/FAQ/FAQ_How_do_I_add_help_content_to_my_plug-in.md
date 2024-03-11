

FAQ How do I add help content to my plug-in?
============================================

Help is added to Eclipse in the form of _help books_ that appear in the online help system. Each help book is laid out in table-of-contents files (toc.xml) that specify the structure of each help book. These table-of-contents files are then registered with the platform, using the org.eclipse.help.toc extension point. The help content must be in the form of standard HTML files. Each major topic should be in a separate file so it can be referenced in the table of contents. Not surprisingly, excellent help content that describes how to integrate help into your plug-in is available, so we don't need to go into further detail here.

  

See Also:
---------

Go to **Platform Plug-in Developer Guide > Programmer's** Guide > Plugging in help**.**

