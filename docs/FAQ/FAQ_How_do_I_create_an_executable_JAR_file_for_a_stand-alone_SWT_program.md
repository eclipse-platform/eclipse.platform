

FAQ How do I create an executable JAR file for a stand-alone SWT program?
=========================================================================

Creating an executable JAR file for a stand-alone SWT program is very similar to creating any executable JAR file, with one important difference. Starting with Eclipse version 3.3, the file "swt.jar" file includes platform-specific executable files that are unpacked automatically when your SWT program is executed. This means that a given runtime folder will normally be platform specific.

The detailed instructions for creating an executable JAR file for a stand-alone SWT file are listed below.

1.  Create a runtime folder for the desired runtime target on your system (e.g., c:\\swt\\runtime-linux). Note that the target platform does not need to be the same as your development platform.
2.  Find the correct SWT JAR file for the desired target platform. You can download the desired ZIP file from [the SWT website](https://www.eclipse.org/swt/). For example, for Eclipse 3.3 and a target platform of Linux, download the file swt-3.3.1.1-gtk-linux-x86.zip. Expand this ZIP file and copy the swt.jar file to the runtime folder. Remember that this swt.jar file is specific to one platform, in this case Linux.
3.  Create a manifest file for your application using the Eclipse text editor (e.g., myapplication-manifest.txt). The text of the manifest should be as follows:
    
                Manifest-Version: 1.0
                Class-Path: swt.jar 
                Main-Class: mypackage.MyClassWithMainMethod
                (blank line at end of file)
    
4.  Make sure the manifest file ends with a blank line. Put the name of your package and class that contains the main() method for the Main-Class.
5.  In Eclipse, select File/Export/Java/Jar file and press Next.
6.  On the JAR File Specification dialog, select the source files for the classes you want in the application. In the export destination, browse to the runtime folder and enter in the desired name of the JAR file (e.g., myapplication.jar or myapplication_linux.jar). Press Next.
7.  On the JAR Packaging Options dialog, make sure the "Export class files with compile warnings" box is checked. Otherwise, if your source files have any compile warnings, they will not be included in the JAR file. Press Next.
8.  In the JAR Export dialog, select the option "Use existing manifest from workspace". Browse to the manifest file you created above. Press Finish.
9.  If the JAR file already exists, you will be asked to overwrite it. Select Yes. If your project had any compile warnings, a message will display. If so, press OK.
10.  At this point, the JAR file for your application has been created in the runtime directory.
11.  If needed (i.e., your target platform is different than your development platform), copy the runtime directory to a directory on your target platform.
12.  In your operating system's file explorer, browse to the runtime directory and run your JAR file. For example, in Windows, you can just double-click on it in the Windows File Explorer or, from the "cmd" prompt, you can enter the command: java -jar myapplication.jar. The application should run.

#### Troubleshooting Errors

A common error message when trying to run a stand-alone application is: "Could not find the main class. Program will exit.", This error can be caused by any of the following problems:

1.  The swt.jar file name is missing or misspelled in the mainfest file.
2.  The swt.jar file is missing from the runtime folder.
3.  The Main-Class: is incorrect in the manifest file.

See Also:
---------

*   [FAQ How do I configure an Eclipse Java project to use SWT?](./FAQ_How_do_I_configure_an_Eclipse_Java_project_to_use_SWT.md "FAQ How do I configure an Eclipse Java project to use SWT?")

