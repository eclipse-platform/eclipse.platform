

FAQ How do I increase the heap size available to Eclipse?
=========================================================

Some JVMs put restrictions on the total amount of memory available on the heap. If you are getting **OutOfMemoryError**s while running Eclipse, the VM can be told to let the heap grow to a larger amount by passing the -vmargs command to the Eclipse launcher. For example, the following command would run Eclipse with a heap size of 2048MB:

eclipse [normal arguments] -vmargs -Xmx2048m [more VM args]

The arguments after -vmargs are directly passed to the VM. Run java -X for the list of options your VM accepts. Options starting with -X are implementation-specific and may not be applicable to all VMs.

You can also put the extra options in `[eclipse.ini](/Eclipse.ini "Eclipse.ini")`.

Here is an example:

        -startup
        plugins/org.eclipse.equinox.launcher_1.5.700.v20200207-2156.jar
        --launcher.library
        plugins/org.eclipse.equinox.launcher.gtk.linux.x86\_64\_1.1.1200.v20200508-1552
        -product
        org.eclipse.epp.package.java.product
        -showsplash
        org.eclipse.platform
        --launcher.defaultAction
        openFile
        --launcher.appendVmargs
        -vmargs
        -Dosgi.requiredJavaVersion=1.8
        -Dosgi.instance.area.default=@user.home/eclipse-workspace
        -XX:+UseG1GC
        -XX:+UseStringDeduplication
        --add-modules=ALL-SYSTEM
        -Dosgi.requiredJavaVersion=1.8
        -Dosgi.dataAreaRequiresExplicitInit=true
        -Xms256m
        -Xmx2048m

See Also:
---------

*   [FAQ How do I run Eclipse?](./FAQ_How_do_I_run_Eclipse.md "FAQ How do I run Eclipse?")

