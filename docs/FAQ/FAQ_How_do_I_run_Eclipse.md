FAQ How do I run Eclipse?
=========================

Contents
--------

*   [1 Starting Eclipse](#Starting-Eclipse)
*   [2 Find the JVM](#Find-the-JVM)
*   [3 eclipse.ini](#eclipse.ini)
*   [4 See Also:](#See-Also:)
*   [5 User Comments](#User-Comments)

Starting Eclipse
----------------

When you unzip Eclipse, the directory layout looks something like this:

      eclipse/
         features/			''the directory containing Eclipse features''
         plugins/			''the directory containing Eclipse plugins''
         eclipse.exe		''platform executable''
         eclipse.ini
         eclipsec.exe              ''(windows only) console executable''
         epl-v10.html		''the EPL license''
         jre/			''the JRE to run Eclipse with''
         notice.html	
         readme	

You can start Eclipse by running eclipse.exe on Windows or eclipse on other platforms. This small launcher essentially finds and loads the JVM. On Windows, the eclipsec.exe console executable can be used for improved command line behavior.

Alternatively, you can launch Eclipse by directly invoking the JVM as follows:

   java -jar eclipse/plugins/org.eclipse.equinox.launcher_1.0.0.v20070606.jar

**NOTES:**

*   The version of org.eclipse.equinox.launcher in the above command must match the version actually shipped with Eclipse. For more details on launching Eclipse using Java (not eclipse.exe) with the launcher, see [Starting Eclipse Commandline With Equinox Launcher](/Starting_Eclipse_Commandline_With_Equinox_Launcher "Starting Eclipse Commandline With Equinox Launcher").
*   When running on **Java ≥ 9**, you may have to make some non-default system modules available, e.g., by adding `--add-modules ALL-SYSTEM` to the command line (please check the release notes on supported Java versions per Eclipse version).

Find the JVM
------------

If a JVM is installed in the eclipse/jre directory, Eclipse will use it; otherwise the launcher will consult the eclipse.ini file and the system path variable. Eclipse **DOES NOT** consult the JAVA_HOME environment variable.

To explicitly specify a JVM of your choice, you can use the -vm command line argument:

      eclipse -vm c:\\jre\\bin\\javaw.exe              ''start Java by executing the specified java executable
      eclipse -vm c:\\jre\\bin\\client\\jvm.dll         ''start Java by loading the jvm in the eclipse process

See the [launcher](/Equinox_Launcher#Finding_a_VM.2C_Using_JNI_Invocation_or_Executing_Java "Equinox Launcher") page for more details on specifying a JVM.

eclipse.ini
-----------

The **most recommended** way to specify a JVM for Eclipse to run in is to put startup configuration into the `[eclipse.ini](/Eclipse.ini "Eclipse.ini")` file in the same folder as the Eclipse executable (`eclipse.exe` on Windows). The Eclipse program launcher will read arguments from either the command-line or the configuration file named `[eclipse.ini](/Eclipse.ini "Eclipse.ini")`. To specify a JVM using configuration file, include the -vm argument in `[eclipse.ini](/Eclipse.ini "Eclipse.ini")`, for example:

      -vm
      c:/jre/bin/javaw.exe

Note: there are no quotes around this path as would be required when executing the same from the command-line were the path to contain white space, etc. This is a common mistake when using Windows.

Eclipse now will launch without additional arguments in the command-line, with the JVM specified in the `[eclipse.ini](/Eclipse.ini "Eclipse.ini")` configuration file.

You should always use -vm so you can be sure of what VM you are using. Installers for other applications sometimes modify the system path variable, thus changing the VM used to launch Eclipse without your knowing about it.

_**The format of the eclipse.ini file is very particular; it is strongly recommended to read**_ [_**eclipse.ini**_](/Eclipse.ini "Eclipse.ini") _**and follow the examples there.**_

When Eclipse starts, you are prompted to choose a workspace location on start-up. This behavior can be configured in the Preferences. You can manually specify the workspace location on the command line, using the -data <workspace-path> command-line argument.

See Also:
---------

*   [FAQ How do I increase the heap size available to Eclipse?](./FAQ_How_do_I_increase_the_heap_size_available_to_Eclipse.md "FAQ How do I increase the heap size available to Eclipse?")
*   [FAQ How do I increase the permgen size available to Eclipse?](./FAQ_How_do_I_increase_the_permgen_size_available_to_Eclipse.md "FAQ How do I increase the permgen size available to Eclipse?")
*   [FAQ Who shows the Eclipse splash screen?](./FAQ_Who_shows_the_Eclipse_splash_screen.md "FAQ Who shows the Eclipse splash screen?")
*   Running Eclipse 3.3M5+

*   [Starting Eclipse Commandline With Equinox Launcher](/Starting_Eclipse_Commandline_With_Equinox_Launcher "Starting Eclipse Commandline With Equinox Launcher")
*   [Automated PDE JUnit Testing With Eclipse 3.3M5](/Automated_PDE_JUnit_Testing_With_Eclipse_3.3M5 "Automated PDE JUnit Testing With Eclipse 3.3M5")


