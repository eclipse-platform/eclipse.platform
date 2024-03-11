

FAQ Where does System.out and System.err output go?
===================================================

Most of the time, the answer is _nowhere_. Eclipse is simply a Java program, and it acts like any other Java program with respect to its output streams. When launched from a shell or command line, the output will generally go back to that shell. In Windows, the output will disappear completely if Eclipse is launched using the javaw.exe VM. When Eclipse is launched using java.exe, a shell window will be created for the output.

Because the output is usually lost, you should avoid using standard output or standard error in your plug-in. Instead, you can log error information by using the platform logging facility. Other forms of output should be written to a file, database, socket, or other persistent store. The only common use of standard output is for writing debugging information, when the application is in debug mode. Read up on the platform tracing facility for more information.

  

See Also:
---------

[FAQ How do I use the platform logging facility?](./FAQ_How_do_I_use_the_platform_logging_facility.md "FAQ How do I use the platform logging facility?")

[FAQ How do I use the platform debug tracing facility?](./FAQ_How_do_I_use_the_platform_debug_tracing_facility.md "FAQ How do I use the platform debug tracing facility?")

