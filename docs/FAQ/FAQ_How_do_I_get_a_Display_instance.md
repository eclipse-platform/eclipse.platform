

FAQ How do I get a Display instance?
====================================

Most users deploy Eclipse as one top-level window and manage their code in perspectives, explorers, editors, and views. However, SWT has been designed to optionally control a multitude of displays wherever this is allowed by the local operating system. The implication is that when creating something like a dialog window, SWT needs to be told _what_ display to use for creating the dialog's frame. Ideally, your application should keep its own references to the display where it is needed, but if for some reason you don't have an instance, you have two ways to find one. The first way is by calling Display.getCurrent. A display is forever tied to the thread that created it, and a thread can have only one active display; a display is active until it is disposed of.

If you call Display.getCurrent, it returns the display that was created in that thread, if any. Here is an example:

      public static Display getDisplay() {
         Display display = Display.getCurrent();
         //may be null if outside the UI thread
         if (display == null)
            display = Display.getDefault();
         return display;		
      }

A calling thread that does not have an active display will return null. Therefore, this method is useful only when you are absolutely certain that you are in the thread that created the display. This brings us to the second way you can obtain a display instance: Display.getDefault(). It will return the first display that was created. If your application has only one display, this is an acceptable way of obtaining the display.

See Also:
---------

[FAQ\_Why\_do\_I\_get\_an\_invalid\_thread\_access_exception?](./FAQ_Why_do_I_get_an_invalid_thread_access_exception.md "FAQ Why do I get an invalid thread access exception?")

