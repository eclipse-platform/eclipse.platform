

FAQ Can I use SWT outside Eclipse for my own project?
=====================================================

This can be interpreted as either a legal question or a technical question. You can find an official answer to the legal question on the SWT FAQ hosted on the SWT development team home page at eclipse.org. The answer to the technical question is an unqualified yes! However, because SWT has a native component, the technical details are a bit more involved than they are for simple Java libraries.

Each platform you want your project to run on will need its own native libraries. Luckily, this is easier than it used to be because the download section of eclipse.org now includes SWT drops. Download the appropriate SWT drop for the platform you are interested in running on, and set up the VM's classpath and library path accordingly. Here is a command line that was used to launch the BrowserSnippet stand-alone program:

      java -cp swt.jar;. -Djava.library.path=. BrowserSnippet

This command line assumes that java is on your execution path and that both swt.jar and the SWT dynamic link library are located in the current working directory.

  

See Also:
---------

*   [FAQ How do I configure an Eclipse Java project to use SWT?](./FAQ_How_do_I_configure_an_Eclipse_Java_project_to_use_SWT.md "FAQ How do I configure an Eclipse Java project to use SWT?")
*   [FAQ How do I create an executable JAR file for a stand-alone SWT program?](./FAQ_How_do_I_create_an_executable_JAR_file_for_a_stand-alone_SWT_program.md "FAQ How do I create an executable JAR file for a stand-alone SWT program?")
*   [FAQ\_How\_is\_Eclipse\_licensed?](./FAQ_How_is_Eclipse_licensed.md "FAQ How is Eclipse licensed?")
*   [FAQ\_How\_do\_I\_display\_a\_Web\_page\_in_SWT?](./FAQ_How_do_I_display_a_Web_page_in_SWT.md "FAQ How do I display a Web page in SWT?")

