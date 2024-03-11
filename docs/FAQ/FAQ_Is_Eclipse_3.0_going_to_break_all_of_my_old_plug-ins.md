

FAQ Is Eclipse 3.0 going to break all of my old plug-ins?
=========================================================

No. Many rumors and discussions circulated during the development of Eclipse 3.0 about how compatible it would be with plug-ins written from Eclipse 2.1 or earlier. Clearly, in the large community of Eclipse plug-in writers, many would be greatly inconvenienced by any breaking changes to existing API. On the other hand, rigidly maintaining API across all releases can be a great barrier to ongoing innovation in the platform. Eventually, a balance was struck that allowed for some well-justified breaking changes, while also providing a compatibility story to allow old plug-ins to continue running on Eclipse 3.0. What does this mean if you have written plug-ins targeting older versions of the platform?

If you do not want to take advantage of new capabilities in Eclipse 3.0, you don't need to do anything. The platform guarantees 99 percent binary compatibility with older versions of Eclipse. Thus, most old plug-ins that used only legal API in previous releases will continue working when installed in Eclipse 3.0. If you find cases in which this is not true, you are encouraged to enter bug reports so that the compatibility support can be fixed.

So far it sounds too easy, right? Well, as the saying goes, "nothing ventured, nothing gained."; If you do want to take advantage of new Eclipse 3.0 API, you will need to do some work to port your plug-in to 3.0. In most cases, the amount of work required is minimal, and the Eclipse plug-in development tools provide utilities for automatically migrating your plug-in manifest file for 3.0. All the required migration is carefully described in the _Eclipse 3.0 Porting Guide_, found in the _Platform Plug-in Developer's Guide_ in the Eclipse help system. If you find that your old code is not compiling or running when being developed against Eclipse 3.0, consult the guide to see what changes might have affected you.

See Also:
---------

[FAQ How do I prevent my plug-in from being broken when I update Eclipse?](./FAQ_How_do_I_prevent_my_plug-in_from_being_broken_when_I_update_Eclipse.md "FAQ How do I prevent my plug-in from being broken when I update Eclipse?")

