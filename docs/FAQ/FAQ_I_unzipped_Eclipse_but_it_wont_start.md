

FAQ I unzipped Eclipse, but it won't start. Why?
================================================

Invariably, with several hundred thousand downloads of Eclipse every month, Eclipse does not start at all for a few users . These failures typically stem from software configuration problems on the host machine or an unusual hardware configuration. If you are already a power Eclipse user, you might be tempted to skip this question. However, even the most advanced Eclipse user will occasionally have problems starting an Eclipse build. So, with a nod to David Letterman, here is a top ten list of the most common start-up problems, along with suggestions for solutions.

  

*   _Cannot find a VM_.

Eclipse requires a JVM to run and does not include one in the download. You need to grab a VM yourself; the Eclipse downloads page has pointers to where you can get one. You may have a VM, but Eclipse cannot find it. To avoid possible conflicts, always specify the VM you are using with the -vm command-line argument.

  

*   _Bad VM_.

All versions of the Eclipse Platform require at least a JDK 1.3 VM. Eclipse 3.0 requires a 1.4 VM. Eclipse 3.3 and later contain many plug-ins that will not work without a 1.5 VM, and some that even require a 1.6 VM; Eclipse will run with a lower VM, but some functionality may be missing. As of Eclipse 3.3, the recommended VM version is 1.5 or later. If you are using a home-grown or experimental JVM and encounter problems, you may not be able to get help from other Eclipse users. Use a reputable VM. If you run into trouble, always try a VM from a major distributor and see whether the problem goes away. Eclipse also may not start if there is a mismatch in the JVM's architecture and the particular Eclipse build you are using. If you are on a 64-bit system but are using a 32-bit JVM, then you should download the 32-bit version of Eclipse. Likewise, if you are using a 64-bit JVM, then you should get the 64-bit version of Eclipse.

  

*   _Unsupported platform_.

Make sure that the architecture and the operating system of your machine match one of the supported systems described in the file readme_eclipse.html. Eclipse will not run on Windows 95 or Commodore 64, for example. If your machine does not match one of the configurations described in the readme, it may still run, but you are on your own!

  

*   _Lack of appropriate native widget toolkit_.

If you download, for example, the GTK version of Eclipse, then you need to make sure that you have GTK (GTK+ 2.2.1 or higher, for Eclipse 3.3) on your computer and that it is correctly installed.

  

*   _Incorrectly unzipped_.

Believe it or not, about once a month, a user reports start-up failure: The user has unzipped Eclipse without selecting the use folder names option. Make sure that the result of unzipping is an install tree with an eclipse directory at its root. The Ark unzip utility in KDE is known to mangle Eclipse zips, so use a different unzip program to install there. The built-in unzip utility in Windows has also been known to have problems, particularly when installing into paths with relatively long names, and may even report (incorrectly) that the zip file requires a password.

  

*   _New Eclipse unzipped on top of older Eclipse_.

Do not do this. Either install Eclipse in a brand new directory or use the Eclipse Update Manager to upgrade an older Eclipse. You can still keep your old workspace. Look in the Eclipse readme file for more details.

  

*   _Buggy build_.

It is not always user error. Some integration builds, and even the odd stable build, will have start-up problems under certain configurations that were not well tested. For example, build 3.0M6 would fail to start up if you restarted with an old workspace after unzipping new plug-ins into the plugins directory. If you are a new user, always start with the most recent official Eclipse release to be sure you are using the least buggy version possible. For more advanced users willing to accept less stable builds, consult Bugzilla to see if your particular start-up problem has already been reported.

  

*   _Xerces problem_.

Prior to Eclipse 3.0, Eclipse used a version of Xerces for parsing XML files, but certain distributions of 1.4 JVMs included a different version of Xerces with the same package names. This should not be a problem with Eclipse 3.0 or higher. See FAQ 108 for more details.

  

*   _Disk full or out of memory_.

Eclipse, especially 2.1 and earlier, does not always gracefully report disk-full errors or out-of-memory errors. Make sure that you have adequate disk space and that you are giving the Java VM enough heap space. See FAQ 26 for details.

  

*   _None of the preceding_.

When all else fails, try asking on the [eclipse.newcomer](https://eclipse.org/forums/eclipse.newcomer) newsgroup or [IRC](/IRC "IRC") channel. Be extremely specific about what operating system, VM, Eclipse build, hardware, and so on, you are running. Attach any error details that you have found in the Eclipse error log. You will find, especially on Linux and Mac, where configuration can be a lot more complicated, that the community is fairly helpful in getting new users going and will generally make an effort in proportion to the effort they perceive that you have made. You are almost guaranteed to get no response if you simply say, Eclipse will not start. If a well-described newsgroup post does not get any response, enter a bug report.

  

  

See Also:
---------

[FAQ How do I increase the heap size available to Eclipse?](./FAQ_How_do_I_increase_the_heap_size_available_to_Eclipse.md "FAQ How do I increase the heap size available to Eclipse?")

  
[FAQ Why doesn't Eclipse play well with Xerces?](./FAQ_Why_doesnt_Eclipse_play_well_with_Xerces.md "FAQ Why doesn't Eclipse play well with Xerces?")

