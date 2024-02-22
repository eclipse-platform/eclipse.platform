FAQ How do I configure an Eclipse Java project to use SWT?
==========================================================

The easiest way to configure a Java project in Eclipse to use SWT is as follows:

1.  Download the SWT stable release for your Eclipse version and your operating system from [Eclipse SWT Project Page](https://www.eclipse.org/swt). For example, for Eclipse version 3.3 and Windows, select the Windows link under Releases / Stable, as shown in the screenshot below.
    
    ![Swt web page.png](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/master/docs/FAQ/images/Swt_web_page.png)
    
2.  This will download a zip file that contains our org.eclipes.swt project. (For example, for Eclipse 3.3 and Windows, the file is called swt-3.3.1.1-win32-win32-x86.zip.) Do not unzip this file. Just download it and note the directory where you saved it.
3.  Inside Eclipse, select Import / Existing Projects into Workspace, as shown below.
    
    ![Import wizard1.png](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/master/docs/FAQ/images/Import_wizard1.png)
    
4.  Press Next and select the option Select archive file. Browse to the zip file you just downloaded. A project called org.eclipse.swt will display in the Projects list. Make sure it is checked, as shown below, and press Finish.
    
    ![Import wizard2.png](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/master/docs/FAQ/images/Import_wizard2.png)
    
5.  Eclipse will create the project org.eclipse.swt in your workspace. This project already has the required SWT JAR file, including source code for the SWT classes.
6.  Select the project that will be used to develop SWT programs (for example, "MyProject) and select Project / Properties / Java Build Path.
7.  Select the Projects tab. Press Add. The org.eclipse.swt project will display in the Select projects to add: list. Select this project by checking the box. The screen should display as shown below.
    
    ![Required project selection.png](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/master/docs/FAQ/images/Required_project_selection.png)
    
8.  Press OK to return to the Projects tab of the Java Build Path dialog. The screen should show the org.eclipse.swt project as shown below.
    
    ![Myproject build path.png](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/master/docs/FAQ/images/Myproject_build_path.png)
    

At this point, your project has access to all of the SWT packages and to the SWT source code.

See Also:
---------

*   [FAQ How do I create an executable JAR file for a stand-alone SWT program?](./FAQ_How_do_I_create_an_executable_JAR_file_for_a_stand-alone_SWT_program.md "FAQ How do I create an executable JAR file for a stand-alone SWT program?")

