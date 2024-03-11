

FAQ What is a working copy?
===========================

The Java model supports the creation of so-called working copies of compilation units. A working copy is an in-memory representation of the compilation unit on disk. Any changes made to the working copy will have no effect on the file on disk until the copy is explicitly reconciled with the original. Using a working copy to manipulate a Java program has a number of powerful advantages over using your own private memory buffer. Most importantly, working copies can be shared between multiple clients. For example, you can programmatically modify the working copy being used by a Java editor, and your changes will appear immediately in the editor. JDT can also detect and report compilation problems on working copies, thus allowing you to preview or warn the user about possible adverse side effects of a change before the copy on disk is modified.</li>

Working copies should generally be used whenever you modify a compilation unit. This ensures that you are modifying the most up-to-date contents of that file, even if they have not yet been written to disk. If you do not use working copies, and a dirty Java editor is open on that file already, the user will be forced to reconcile your changes manually. The following example of using a working copy to replace a compilation unit's contents is from the implementation of ChangeReturnTypeAction included in this book's FAQ Examples plug-in:

      ICompilationUnit unit = ...; //get compilation unit handle
      unit.becomeWorkingCopy(null, null);
      try {
         IBuffer buffer = unit.getBuffer();
         String oldContents = buffer.getContents();
         String newContents = ...; //make some change
         buffer.setContents(newContents);
         unit.reconcile(false, null);
      } finally {
         unit.discardWorkingCopy();
      }

You should always put discardWorkingCopy in a finally block to ensure that the working copy opened by becomeWorkingCopy is discarded even in the case of an exception. Although in this example, we simply replaced the old file contents with new contents, the IBuffer API can be used to perform modifications on smaller parts of the buffer, to replace a region, or to append contents to the end of the file.

See Also:
---------

*   [FAQ How do I manipulate Java code?](./FAQ_How_do_I_manipulate_Java_code.md "FAQ How do I manipulate Java code?")
*   [FAQ What is a JDOM?](./FAQ_What_is_a_JDOM.md "FAQ What is a JDOM?")

