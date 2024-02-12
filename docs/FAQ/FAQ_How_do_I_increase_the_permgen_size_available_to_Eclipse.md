FAQ How do I increase the permgen size available to Eclipse?
============================================================

Note: Oracle Java 8 does not have a separate permanent generation space any more. The -XX:(Max)PermSize option makes no difference (the JVM will ignore it, so it can still be present).

If you see `java.lang.OutOfMemoryError: PermGen space` errors, you need to increase the permanent generation space available to Eclipse.

PermGen is the permanent generation of objects in the VM (Class names, internalized strings, objects that will never get garbage-collected). An easy, if somewhat memory-hungry fix is to enlarge the maximum space for these objects by adding

        -XX:MaxPermSize=128M

as an argument to the JVM when starting Eclipse. 
The recommended way to do this is via your `eclipse.ini` file.

Alternatively, you can invoke the Eclipse executable with command-line arguments directly, as in

        eclipse [normal arguments] -vmargs -XX:PermSize=64M -XX:MaxPermSize=128M [more VM args]

Note: The arguments after -vmargs are directly passed to the VM. Run java -X for the list of options your VM accepts. Options starting with -X are implementation-specific and may not be applicable to all JVMs (although they do work with the Sun/Oracle JVMs).

Eclipse and Sun VMs on Windows
------------------------------

Eclipse 3.3 and above supports an argument to the launcher: --launcher.XXMaxPermSize. On Windows, Eclipse ships with the following lines in the `eclipse.ini` file:

        --launcher.XXMaxPermSize
        256m

With the above arguments, if the VM being used is a Sun VM and there is not already a -XX:MaxPermSize= VM argument, then the launcher will automatically add -XX:MaxPermSize=256m to the list of VM arguments being used. The Eclipse launcher is only capable of identifying Sun VMs on Windows.

The option --launcher.XXMaxPermSize is something that the launcher reads (not the JVM); it tells the launcher to automatically size the JVM's perm gen if it (the launcher) detects a Sun JVM that supports that option. This alleviates the need to put it under -vmargs (where non-Sun JVM's could fail because they don't understand that option).

Note: Eclipse 3.6 and below on Windows has a bug with Oracle/Sun JDK 1.6.0_21 (July 2010) where the launcher cannot detect a Oracle/Sun VM, and therefore does not use the correct PermGen size. If you are using either of this version, add the -XX flag to the eclipse.ini as described above.

Note: Eclipse 3.3.1 has a bug where the launcher cannot detect a Sun VM, and therefore does not use the correct PermGen size. It seems this may have been a known bug on Mac OS X for 3.3.0 as well. If you are using either of these platform combinations, add the -XX flag to the eclipse.ini as described above.

See Also:
---------

[FAQ How do I run Eclipse?](./FAQ_How_do_I_run_Eclipse.md)