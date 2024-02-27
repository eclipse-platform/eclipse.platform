# NOTE: LOTS OF QUESTIONS AND ANSWERS ARE NOT UP TO DATE

# This document is based on frequently asked questions in a older support channel of Eclipse called IRC and was located in a wiki which has been deprecreated
# In Febr. 2024 the most outdated content has been removed or updated but if you see something which you can improve / update please send a PR for this repo



IRC FAQ
=======

A collection of FAQs gathered in the Eclipse [IRC](/IRC "IRC") channels. Some of them are logged, see the specific channel for details.

Contents
--------

*   [1 General](#General)
    *   [1.1 I'm new, what should I read first?](#im-new-what-should-i-read-first)
*   [2 Installation, Startup and Runtime](#installation-startup-and-runtime)
    *   [2.1 Where can I get Eclipse?](#Where-can-I-get-Eclipse)
    *   [2.2 What's the difference between all the different packages like 'Eclipse IDE for Java Developers' and the 'Eclipse IDE for Java EE Developers'? What do they contain? Do they contain source code?](#whats-the-difference-between-all-the-different-packages-what-do-they-contain-do-they-contain-source-code)
    *   [2.3 How do I verify my download? Are there any MD5 or SHA1 hashes for me to verify my download against?](#how-do-i-verify-my-download-are-there-any-md5-or-sha1-hashes-for-me-to-verify-my-download-against)
    *   [2.4 What are all these strangely named releases?](#What-are-all-these-strangely-named-releases)
    *   [2.5 Where can I get a list of all the Eclipse projects?](#Where-can-I-get-a-list-of-all-the-Eclipse-projects)
    *   [2.6 Where can I get project XYZ?](#Where-can-I-get-project-XYZ)
        *   [2.6.1 Is there a GUI Builder?](#Is-there-a-GUI-Builder)
    *   [2.7 What is p2?](#What-is-p2)
    *   [2.8 How do I start Eclipse?](#How-do-I-start-Eclipse)
    *   [2.9 How do I upgrade/update Eclipse?](#how-do-i-upgradeupdate-eclipse)
    *   [2.10 What other command line arguments are available?](#What-other-command-line-arguments-are-available)
    *   [2.11 How do I debug Eclipse? How can I see what plug-ins are being started? Why aren't the plug-ins I installed showing up in the UI? How do I start the OSGi console?](#how-do-i-debug-eclipse-how-can-i-see-what-plug-ins-are-being-started-why-arent-the-plug-ins-i-installed-showing-up-in-the-ui-how-do-i-start-the-osgi-console)
        *   [2.11.1 Debugging OSGi Bundle Loading Issues](#Debugging-OSGi-Bundle-Loading-Issues)
        *   [2.11.2 Debugging Eclipse Using Eclipse](#Debugging-Eclipse-Using-Eclipse)
            *   [2.11.2.1 Launching as an Eclipse Application using PDE](#Launching-as-an-Eclipse-Application-using-PDE)
            *   [2.11.2.2 Attaching to a running instance](#Attaching-to-a-running-instance)
    *   [2.12 Can I use my Eclipse workspace from an old release with a new Eclipse release?](#Can-I-use-my-Eclipse-workspace-from-an-old-release-with-a-new-Eclipse-release)
    *   [2.13 How do I use a different workspace?](#How-do-I-use-a-different-workspace)
    *   [2.14 When I start Eclipse it says "Workspace in use or cannot be created, choose a different one.", what should I do?](#when-i-start-eclipse-it-says-workspace-in-use-or-cannot-be-created-choose-a-different-one-what-should-i-do)
    *   [2.15 How do I copy plugins between Eclipse installations with p2?](#How-do-I-copy-plugins-between-Eclipse-installations-with-p2)
    *   [2.16 How do I install a plug-in with multiple dependencies?](#How-do-I-install-a-plug-in-with-multiple-dependencies.3F)
*   [3 Crashers, Freezing, and other Major Issues](#Crashers.2C-Freezing.2C-and-other-Major-Issues)
    *   [3.1 I'm having memory, heap, or permgen problems, what can I do?](#I.27m-having-memory.2C-heap.2C-or-permgen-problems.2C-what-can-I-do.3F)
    *   [3.2 Eclipse seems to be hanging on startup. How can I find out why?](#Eclipse-seems-to-be-hanging-on-startup.-How-can-I-find-out-why.3F)
    *   [3.3 Update complains that it cannot find a repository](#Update-complains-that-it-cannot-find-a-repository)
	*	[3.4 Eclipse keeps running out of (permgen) memory when I use Oracle/Sun's Java 6 update 21 on Windows](eclipse-keeps-running-out-of-permgen-memory-when-i-use-oraclesuns-java-6-update-21-on-windows)
	*	[3.5 After startup, i see only an empty Dialog - eclipse won't start](after-startup-i-see-only-an-empty-dialog---eclipse-wont-start)
*   [4 Eclipse](#Eclipse)
    *   [4.1 How do I create a project for an existing source directory?](#How-do-I-create-a-project-for-an-existing-source-directory)
        *   [4.1.1 Option 1: Import the source into an existing project](#option-1-import-the-source-into-an-existing-project)
        *   [4.1.2 Option 2: Create project on the existing source directory](#option-2-create-project-on-the-existing-source-directory)
        *   [4.1.3 Option 3: Create project and link to existing source](#option-3-create-project-and-link-to-existing-source)
    *   [4.2 Where are Eclipse's log files located?](#where-are-eclipses-log-files-located)
    *   [4.3 I was working on a project and doing something or other does not work. Where should I start?](#I-was-working-on-a-project-and-doing-something-or-other-does-not-work.-Where-should-I-start)
    *   [4.4 Where are Eclipse preferences stored?](#Where-are-Eclipse-preferences-stored)
    *   [4.5 Where are update site bookmarks stored?](#Where-are-update-site-bookmarks-stored)
    *   [4.6 Where are my Eclipse plug-ins folder?](#Where-are-my-Eclipse-plug-ins-folder)
    *   [4.7 What's the key for ...?](#whats-the-key-for-F)
        *   [4.7.1 How do I add my own bindings?](#How-do-I-add-my-own-bindings)
        *   [4.7.2 Why can't I find the command I'm looking for?](#Why-cant-I-find-the-command-Im-looking-for)
    *   [4.8 Why did Content Assist stop working?](#Why-did-Content-Assist-stop-working)
    *   [4.9 Why won't Content Assist work for my .xyz file type?](#why-wont-content-assist-work-for-my-xyz-file-type)
    *   [4.10 How do I manually assign a project Nature or BuildCommand?](#How-do-I-manually-assign-a-project-Nature-or-BuildCommand)
    *   [4.11 How do I export a launch configuration?](#How-do-I-export-a-launch-configuration)
    *   [4.12 How do I find out which workspace I currently have open?](#How-do-I-find-out-which-workspace-I-currently-have-open)
    *   [4.13 Why is Eclipse launching the current file I have open instead of whatever I last launched?](#Why-is-Eclipse-launching-the-current-file-I-have-open-instead-of-whatever-I-last-launched)
    *   [4.14 How do I configure Eclipse to use a black background with a white font?](#How-do-I-configure-Eclipse-to-use-a-black-background-with-a-white-font)
    *   [4.15 Where do I find the javadoc for the Eclipse API locally? I don't always want to load stuff up in a browser.](#Where-do-I-find-the-javadoc-for-the-Eclipse-API-locally-I-dont-always-want-to-load-stuff-up-in-a-browser)
    *   [4.16 Cut/Copy/Paste does not appear to be working properly on Linux. It's not often that I have to invoke the keyboard shortcut multiple times for it to take effect. What's the deal here?](#cutcopypaste-does-not-appear-to-be-working-properly-on-linux-its-not-often-that-i-have-to-invoke-the-keyboard-shortcut-multiple-times-for-it-to-take-effect-whats-the-deal-here)
    *   [4.17 How do I show line numbers in the Eclipse text editor?](#How-do-I-show-line-numbers-in-the-Eclipse-text-editor)
    *   [4.18 How do I change the colour of the highlighting marker that highlights all the occurrences of some element in the text editor?](#How-do-I-change-the-colour-of-the-highlighting-marker-that-highlights-all-the-occurrences-of-some-element-in-the-text-editor)
    *   [4.19 How do I switch my workspace?](#How-do-I-switch-my-workspace)
    *   [4.20 I have just installed a plug-in but I do not see any indication of it in my workspace. What do I do?](#I-have-just-installed-a-plug-in-but-I-do-not-see-any-indication-of-it-in-my-workspace-What-do-I-do)
    *   [4.21 How do I check for the command line invocation that Eclipse used to launch an application?](#How-do-I-check-for-the-command-line-invocation-that-Eclipse-used-to-launch-an-application)
    *   [4.22 Can projects exist outside of the workspace's folder?](#Can-projects-exist-outside-of-the-workspaceS-folder)
    *   [4.23 How do I change the list of workspaces listed under the 'Switch Workspace' submenu?](#how-do-i-change-the-list-of-workspaces-listed-under-the-switch-workspace-submenu)
    *   [4.24 How do I recover my saved passwords from the .keyring file?](#how-do-i-recover-my-saved-passwords-from-the-keyring-file)
    *   [4.25 My line delimiter changes are not being persisted to the file. What's going on?](#My-line-delimiter-changes-are-not-being-persisted-to-the-file.-Whats-going-on)
    *   [4.26 Black background color for tooltips on Linux/Ubuntu/GTK](#black-background-color-for-tooltips-on-linuxubuntugtk)
    *   [4.27 Excessive tab folder height on Linux/Ubuntu/GTK](#excessive-tab-folder-height-on-linuxubuntugtk)
    *   [4.28 How can I easily migrate settings and preferences between my Eclipse workspaces?](#How-can-I-easily-migrate-settings-and-preferences-between-my-Eclipse-workspaces)
    *   [4.29 How do I swap between different programs' output in the 'Console' view? How do I open another 'Console' view?](#how-do-i-swap-between-different-programs-output-in-the-console-view-how-do-i-open-another-console-view)
*   [5 Java Development Tools (JDT)](#Java-Development-Tools-JDT)
    *   [5.1 The javadoc for the standard Java classes does not show up as context help. What is the problem? Should I download the javadocs?](#the-javadoc-for-the-standard-java-classes-does-not-show-up-as-context-help-what-is-the-problem-should-i-download-the-javadocs)
        *   [5.1.1 What do you mean by 'run a JDK'?](#what-do-you-mean-by-run-a-jdk)
        *   [5.1.2 But it still does \*not\* work! Help me!](#but-it-still-does-not-work-help-me)
        *   [5.1.3 But I'm on MacOS X which comes with the JDK](#But-im-on-MacOS-X-which-comes-with-the-JDK)
    *   [5.2 How do I override the environment variables that Ant uses during execution?](#How-do-I-override-the-environment-variables-that-Ant-uses-during-execution)
    *   [5.3 Why is Content Assist not working in the Java editor? Why doesn't Eclipse recognize my .java file as a Java file?](#Why-is-Content-Assist-not-working-in-the-Java-editor-Why-doesnt-Eclipse-recognize-my-java-file-as-a-Java-file)
    *   [5.4 How do I change the Java compiler compliance level for my workspace?](#How-do-I-change-the-Java-compiler-compliance-level-for-my-workspace)
    *   [5.5 How do I add arguments to the Java program I am running?](#How-do-I-add-arguments-to-the-Java-program-I-am-running)
    *   [5.6 How do I alter my package representation so that parent packages are housing child packages?](#How-do-I-alter-my-package-representation-so-that-parent-packages-are-housing-child-packages)
    *   [5.7 I clicked on something and now I can only see the method that I am currently editing. What do I do? Did I lose my entire file?](#I-clicked-on-something-and-now-I-can-only-see-the-method-that-I-am-currently-editing-What-do-I-do-Did-I-lose-my-entire-file)
    *   [5.8 I would like code completion to be activated as I type like how it works in Visual Studio? Can I turn this on somewhere?](#I-would-like-code-completion-to-be-activated-as-I-type-like-how-it-works-in-Visual-Studio-Can-I-turn-this-on-somewhere)
    *   [5.9 I've been told that Eclipse has its own Java compiler, is this true? Can I use Sun's javac instead?](#Ive-been-told-that-Eclipse-has-its-own-Java-compiler-is-this-true-Can-I-use-Suns-javac-instead)
    *   [5.10 I call System.console() in my code but null is returned. It does work in the command line though. What's going on?](#i-call-systemconsole-in-my-code-but-null-is-returned-it-does-work-in-the-command-line-though-whats-going-on)
    *   [5.11 Why isn't my { class | jdbc driver | ... } being found?](#why-isnt-my--class--jdbc-driver----being-found)
*   [6 PHP Development Tools (PDT)](#PHP-Development-Tools-PDT)
    *   [6.1 When I try to create PHP project, I get an error saying "Creation of element failed.", what should I do?](#when-i-try-to-create-php-project-i-get-an-error-saying-creation-of-element-failed-what-should-i-do)
*   [7 Plug-in Development](#Plug-in-Development)
    *   [7.1 How do I test my plug-ins?](#How-do-I-test-my-plug-ins)
    *   [7.2 I get an unhandled event loop exception in my console. What gives?](#I-get-an-unhandled-event-loop-exception-in-my-console-What-gives)
    *   [7.3 A plug-in in my 'Eclipse Application' launch configuration is listed as being "out of sync", what should I do?](#a-plug-in-in-my-eclipse-application-launch-configuration-is-listed-as-being-out-of-sync-what-should-i-do)
    *   [7.4 I added a jar to my classpath, but it's not being found! What should I do?](#i-added-a-jar-to-my-classpath-but-its-not-being-found-what-should-i-do)
    *   [7.5 How do I find the source for a plugin?](#How-do-I-find-the-source-for-a-plugin)
    *   [7.6 How do I edit the source for a plugin?](#How-do-I-edit-the-source-for-a-plugin)
*   [8 SWT](#SWT)
    *   [8.1 I cannot get the SWT widget ABC to work when I do XYZ. Could you help me?](#i-cannot-get-the-swt-widget-abc-to-work-when-i-do-xyz-could-you-help-me)

General
-------

### I'm new, what should I read first?

*   [Recommended Eclipse Reading List](http://www.ibm.com/developerworks/opensource/library/os-ecl-read/)
*   [The Official Eclipse FAQs - Getting Started](/The_Official_Eclipse_FAQs#Getting_Started "The Official Eclipse FAQs")

Installation, Startup and Runtime
---------------------------------

### Where can I get Eclipse?

*   Use the [downloads page](http://www.eclipse.org/downloads/) or [download package](https://www.eclipse.org/downloads/packages/) to get the latest releases. 


### What's the difference between all the different packages? What do they contain? Do they contain source code?

They contain different plug-ins, see the descriptive text for each package.

### How do I verify my download? Are there any MD5 or SHA1 hashes for me to verify my download against?

Eclipse.org offers MD5 and SHA1 hashes for the downloads on the main [downloads page](http://www.eclipse.org/downloads/). Click the 'More...' link for the download you are interested in and then find the 'Checksums...' link on the right hand side.

### What are all these strangely named releases?

Eclipse uses the year and the month as release name, for example 2024-12 would be the december release 2024.

### Where can I get a list of all the Eclipse projects?

See

*   [Official Eclipse Projects](https://www.eclipse.org/projects/)
*   The [Eclipse Marketplace](https://marketplace.eclipse.org/)

### Where can I get project XYZ?

You can search for downloads [by project](https://www.eclipse.org/downloads/index_project.php) or [by topic](https://www.eclipse.org/downloads/index_topic.php).

Or, start with that project's homepage  and look for download or update links. For example, EMF: [website](http://www.eclipse.org/modeling/emf/), [downloads](http://www.eclipse.org/modeling/emf/downloads/), [updates](http://www.eclipse.org/modeling/emf/updates/).

Download the zip and unpack it into your Eclipse install folder, or use a .link file [\[1\]](http://divby0.blogspot.com/2007/06/managing-plugins-and-features-with-link.html) [\[2\]](http://www.ibm.com/developerworks/opensource/library/os-ecl-manage/) to locate the project in another folder.

Or, if using Update Manager, add the Update URL here, then download and install the features and plugins that way:

    Help > Install New Software > Past the URL into the Work with field
   Now select what you want to install



#### Is there a GUI Builder?

[WindowBuilder](https://eclipse.org/windowbuilder/) supports round-tripping of SWT and Swing designs.

### What is p2?

p2 is a provisioning system allowing to install and update Eclipse components.

### How do I start Eclipse?

The simplest way:

*   Double-click _eclipse.exe_ or the _Eclipse_ application, or
*   Browse to the directory where you unpacked Eclipse, then run `eclipse` or `./eclipse`

If you need to add additional command-line parameters, then you will need to get to a command prompt and run something like:

    /path/to/eclipse/eclipse -data /path/to/workspace -vm /path/to/jvm/bin/java -vmargs -Xms256M -Xmx512M -XX:PermSize=64M -XX:MaxPermSize=128M 
    

 

\- or -

    c:\path\to\eclipse\eclipse.exe -data c:\path\to\workspace -vm c:\path\to\jvm\bin\java.exe -vmargs -Xms256M -Xmx512M -XX:PermSize=64M -XX:MaxPermSize=128M 
    

 

Note that any arguments for the JVM, including setting properties, must come after the _-vmargs_.

Using eclipse.ini:

*   See [this](./FAQ_How_do_I_run_Eclipse%3F#eclipse.ini.md "FAQ How do I run Eclipse?") or [that](/Eclipse.ini "Eclipse.ini").

Other ways:

*   See [FAQ How do I run Eclipse?](./FAQ_How_do_I_run_Eclipse.md "FAQ How do I run Eclipse?")

Advanced ways:

*   See [Starting Eclipse Commandline With Equinox Launcher](/Starting_Eclipse_Commandline_With_Equinox_Launcher "Starting Eclipse Commandline With Equinox Launcher"). See also [Category:Equinox](/Category:Equinox "Category:Equinox").

### How do I upgrade/update Eclipse?

See [here](./FAQ_How_do_I_upgrade_Eclipse.md "FAQ How do I upgrade Eclipse?").

### What other command line arguments are available?

*   [https://www.eclipse.org/swt/launcher.html](http://www.eclipse.org/swt/launcher.html)
*   [Eclipse runtime options](https://help.eclipse.org/indigo/topic/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html)

### How do I debug Eclipse? How can I see what plug-ins are being started? Why aren't the plug-ins I installed showing up in the UI? How do I start the OSGi console?

Having problems starting Eclipse or getting certain plug-ins to load?

#### Debugging OSGi Bundle Loading Issues

There are a few flags you can pass to Eclipse [on the commandline or in your eclipse.ini file](#How-do-I-start-Eclipse.3F) that might help:

*   **-consolelog** \- log everything in workspace/.metadata/.log to the console where you launched Eclipse as well
*   **-debug** \- more verbose console output
*   **-console** \- start the [Equinox OSGi console](http://en.wikipedia.org/wiki/Equinox_OSGi) to interact with OSGi directly
*   **-noexit** \- when Eclipse closes, keep the OSGi console running until you type 'exit' or hit Ctrl+C so you can keep debugging

See [Where Is My Bundle?](/Where_Is_My_Bundle "Where Is My Bundle") for an overview of how to use the OSGi console for diagnosing problems.

#### Debugging Eclipse Using Eclipse

You can also debug an Eclipse instance from another instance through remote debugging. We usually refer the Eclipse instance driving the debugging session as the "outer" session, and the instance being debugged as the "inner" session. If your situation is dependent on your workspace settings, you'll need to run your outer instance from a different workspace so that your inner instance can use the problematic workspace.

You will often want to set a breakpoint somewhere to have the inner instance stop at some key point. For example, to debug why the workbench window's toolbar is being shown or hidden, which is controlled within the WorkbenchWindow class, you would first do the following:

1.  First we have to make the applicable classes available.
    1.  Open the _Plug-Ins_ view (_Window → Show View → Other..._)
    2.  Scroll to find the applicable bundles. In this case, we're looking for org.eclipse.ui.workbench. Right-click on the bundles and choose _Add To Java Search Path_. This makes the bundle classes visible to JDT.
2.  Use Open Type (Ctrl+Shift+T) to open the applicable classes (e.g., WorkbenchWindow).
3.  Place breakpoints on the applicable methods or fields (e,g, updateLayoutDataForContent()).

Next we need to launch or attach to the inner instance. Launching a new instance using the Plug-in Development Environment (PDE) is easier, but sometimes problems only happen when run as a fully deployed application.

##### Launching as an Eclipse Application using PDE

The simplest approach is to launch a new Eclipse application from within Eclipse. You need to create a _launch configuration_ to configure the set of bundles for Eclipse:

1.  Open _Run → Debug Configurations..._ to open the Launch Configuration dialog.
2.  Select the "Eclipse Application" group item in the left-hand pane, right click and select "New".
3.  In the Main tab: in the _Workspace Data_ area, specify your original workspace as the Location. Ensure the _Program To Run_ area specifies a product _org.eclipse.sdk.ide_.
4.  You may need to adjust other entries.
5.  Finally, click on the Debug button.

##### Attaching to a running instance

This approach requires that the running instance be started with some special flags to allow attaching the debugger.

1.  Start the instance to be debugged with "-vmargs -agentlib:jdwp=transport=dt_socket,server=y,address=8000". You should see a message like "Listening for transport dt_socket at address: 8000"
2.  Open _Run → Debug Configurations..._ and create a _Remote Java Application_ configuration with connection type "Socket Attach" and connecting to the client at port 8000. Set the project to a bundle project with the right dependencies for the bundles that you are trying to debug.
3.  Launch the configuration.

The JDWP agent supports other useful arguments, like "suspend=n" so that the process does not suspend. For more details, see Oracle's [Java Debug Wire Protocol (JDWP) connection docs](http://docs.oracle.com/javase/8/docs/technotes/guides/jpda/conninv.html). See also: [Debug Java applications remotely with Eclipse](http://www.ibm.com/developerworks/library/os-eclipse-javadebug/) or [Hacking the Java Debug Wire Protocol](http://blog.ioactive.com/2014/04/hacking-java-debug-wire-protocol-or-how.html)


### Can I use my Eclipse workspace from an old release with a new Eclipse release?

Yes and no.

Your project files are compatible, but some of your settings may not be. You might want to export your settings from the old workspace before attempting to open it with the new Eclipse, then import them into the new Eclipse. For example, `Window > Preferences... > Java > Code Style > Formatter > Edit > Export`.

You might also want to start a completely new workspace and use `File > Import > Existing Projects into Workspace` to migrate the old projects into the new workspace. They can be copied or simply linked (referenced) in their old workspace location.

### How do I use a different workspace?

Three ways:

1.  In Eclipse, select `File > Switch Workspace`
2.  In Eclipse, select `Window > Preferences... > General > Startup and Shutdown > \[x\] Prompt for workspace on startup`, then restart Eclipse.
3.  Via commandline, run `./eclipse -data /path/to/new/workspace/folder`


### When I start Eclipse it says "Workspace in use or cannot be created, choose a different one.", what should I do?

There are a couple of things you can try.

1.  Delete the `workspace/.metadata/.lock` file.
2.  Check your running processes to make sure there aren't any remaining Java or Eclipse processes running. When in doubt, restart your computer. :)
3.  Try starting Eclipse on a different workspace (from the workspace selection dialog, or by using a command line argument like `-data /home/user/tmp-workspace`), then switch 
back to your original workspace.

If none of these solution work, could you be trying to create the workspace on a folder mounted via NFS? If yes, please make sure you are using NFS v4.

### How do I copy plugins between Eclipse installations with p2?

To copy plugins from installation A to B you can do the following:

1.  Start A
2.  Go to Help->Install new Software
3.  Add a new local update site that points to <path\_to\_B_eclipse>/p2/org.eclipse.equinox.p2.engine/profileRegistry/<profile>.profile/
4.  Untick the 'Group items by category' checkbox and optionally tick 'Hide items that are already installed'
5.  Select the plugins you want to import and follow the wizard

step-by-step instructions (including screenshots) can be found here: [http://www.peterfriese.de/following-eclipse-milestones/](http://www.peterfriese.de/following-eclipse-milestones/)

To receive updates for the plugins you copied, you also have to copy their update sites:

1.  Start B
2.  Go to preferences -> Install/Update -> Available Software Sites
3.  Export all sites for the plugins you copied
4.  Start A
5.  Go to preferences -> Install/Update -> Available Software Sites
6.  Import the list you exported


### How do I uninstall a plug-in?

You can view your list of installed software by checking your installation details from about dialog.

*   Help > About > Installation Details

Crashers, Freezing, and other Major Issues
------------------------------------------

Do you have any `hs\_err\_pid*log` files lying around? This is an error log that is generated by HotSpot, Sun's implementation of the Java Virtual Machine (assuming that is the JVM you are using). HotSpot is also the JVM being used by the OpenJDK project, HotSpot is also being used by IcedTea (which has since been renamed to OpenJDK also).

### I'm having memory, heap, or permgen problems, what can I do?

*   [FAQ How do I increase the heap size available to Eclipse?](./FAQ_How_do_I_increase_the_heap_size_available_to_Eclipse.md "FAQ How do I increase the heap size available to Eclipse?")
*   [FAQ How do I increase the permgen size available to Eclipse?](./FAQ_How_do_I_increase_the_permgen_size_available_to_Eclipse.md "FAQ How do I increase the permgen size available to Eclipse?")


### Eclipse seems to be hanging on startup. How can I find out why?

If none of the solutions outlined in this section reveal the problem, then you can try debugging an Eclipse instance as a debug target from another Eclipse instance. This is surprisingly easy:

1.  Start Eclipse in a "new" blank workspace (e.g., C:\\TEMP\\WS, or /tmp)
2.  Create a new Debug configuration: Run -> Debug Configurations; then click on "Eclipse Applications" and select the New Launch Configuration.
    1.  If you believe it's something about a particular workspace, then set the workspace to your normal workspace.
    2.  If you believe the hang is caused by a particular plugin, disable the plugin and verify.
3.  Launch and then see.

Using this approach, you can break with the debugger to see where hangs are occurring. You can also change the selection of plugins that the instance is launched with.

### Update complains that it cannot find a repository

A number of Ubuntu/Linux users have complained about the update manager being unable to find a repository. There is an [Ubuntu bug](https://bugs.launchpad.net/ubuntu/+source/eclipse/+bug/541482) tracking the issue with several possible solutions. If none of those solutions resolve your issue:

1.  Eliminate connectivity problems to the Eclipse update site by fetching [http://download.eclipse.org/releases/galileo/compositeContent.jar](http://download.eclipse.org/releases/galileo/compositeContent.jar)
2.  Manually specify a mirror: this [link](http://www.eclipse.org/downloads/download.php?format=xml&file=/releases/galileo/&protocol=http) returns an xml-encoded list of mirrors for the Galileo release; replace the "/releases/galileo" with the update location for other update sites.

### Eclipse keeps running out of (permgen) memory when I use Oracle/Sun's Java 6 update 21 on Windows

The Eclipse launcher is currently ignoring the VM arguments in the `eclipse.ini` file due to the change in branding of the HotSpot VM from Sun to Oracle. The workaround is to set the argument directly via the command line or to append it to your shortcut/script.

    eclipse.exe -vmargs -XX:MaxPermSize=256m
    

 

Please see [bug 319514](https://bugs.eclipse.org/bugs/show_bug.cgi?id=319514) for more details.

### After startup, i see only an empty Dialog - eclipse won't start

On the .log-File (workbench/.metadata/.log) i see an error like:

 

     The workspace exited with unsaved changes in the previous session; refreshing workspace to recover changes.
    

 

So check if the file **.snap** exist in your Workbench-Folder on: .metadata/.plugins/org.eclipse.core.resources. Than remove, or rename the file and start eclipse again.  
  

Eclipse
-------

### How do I create a project for an existing source directory?

If the source was already a project, use the _File → Import → General → Existing Project into Workspace_.

If the source has never been imported as a project previously, or you do not have the Eclipse project metadata, you have three ways to create a project:

1.  Copy (or Import) the source into an existing project
2.  Create a new project but placing the project on the existing source directory
3.  Create a new project and link to the existing source

The following assumes a Java project, but other language toolings should behave similarly.

#### Option 1: Import the source into an existing project

There are two ways to import source into an existing project. If you do not have an existing project, create one using the _File → New → Java Project_ wizard.

The first, and easiest, is to import folders and files by dragging them from the file system and dropping them into one of the navigation views, or by copying and pasting.

The second method is to use the Import wizard:

1.  _File → Import → File System_
2.  Specify the root of the source folder.
3.  Specify the folder within the project where the files should be copied to. Typically you would choose the the 'src' directory in a Java project
4.  Select _Finish_

Although you can select individual files, the directory hierarchy shown is duplicated in the destination _exactly as shown_. Although considered counter-intuitive to many, the folder-name of the source is included as part of the destination. If you are importing Java source code from .../project/src/com/example/..., you must specify the source directory as ".../project/src/com" to avoid "src" being included as the root.

#### Option 2: Create project on the existing source directory

1.  File → New → Java Project
2.  Untick the "Use default location" and specify the location of your source.
3.  Click _Next_. JDT will examine the directory layout and propose build settings. If you specified the root of your source tree, then the class files will be placed alongside your source files. If you specify the parent of your source, then by default the class files will be placed in a sibling called 'bin'. These decisions can be altered in the build settings view.
4.  Click _Finish_

#### Option 3: Create project and link to existing source

1.  File → New → Java Project
2.  Provide the project name, and then click _Next_
3.  Select the _Link additional source_.
4.  In the _Link Source_ dialog, provide the location of where your source is found on disk (e.g., .../project/src), and the name of the linked folder within your new project (e.g., "src-linked").
5.  Click _Finish_

Note that this approach causes the Eclipse metadata to be stored separately from the source files.

### Where are Eclipse's log files located?

*   `<workspace>/.metadata/.log`

You can view this workspace log as a view if you have PDE installed on your computer (which you would if you have downloaded the Eclipse SDK). You can open that view via Window -> Show View -> Other -> PDE Runtime -> Error Log.

*   `<eclipse install>/configuration/<sometimestamp>.log`
*   `<eclipse install>/configuration/org.eclipse.update/install.log`
*   VM crashes can produce `hs\_err\_pid*.log` files (Oracle VMs) or write something to `~/.xsession-errors` (Linux)

### I was working on a project and doing something or other does not work. Where should I start?

1.  Try refreshing your projects.
2.  Try cleaning your your projects using the menu item Project/Clean to trigger a rebuild.
3.  Try closing/reopening your projects.
4.  Try restarting Eclipse.

### Where are Eclipse preferences stored?

If you want to keep preferences from one version to the other, export them using File/Export/Preferences.

Preferences are stored in various places (this applies to Eclipse 3.1)

*   for each installation (but this may vary for multi-user installations), in files stored in <eclipse_home>/eclipse/configuration/.settings/ . There is typically one file per plugin, with a prefs extension. Note that very few plug-ins use installation-wide preferences.
*   for each workspace, in files stored in <workspace>/.metadata/.plugin/org.eclipse.core.runtime/.settings . There is typically one file per plugin, with a prefs extension.
*   for each project --for project-level settings -- in files stored in a .settings sub-directory of your project folder

### Where are update site bookmarks stored?

It is within an XML file called <user\_home>/.eclipse/org.eclipse.platform\_3.1.2/configuration/org.eclipse.update/bookmarks.xml. Your Eclipse version may vary.

### Where are my Eclipse plug-ins folder?

The plug-ins folder is <eclipse_home>/plugins. Starting with Eclipse 3.4, with the advent of [p2](/Equinox/p2 "Equinox/p2"), you should put plug-ins in the [dropins/](/Equinox_p2_Getting_Started#Dropins "Equinox p2 Getting Started") folder.

### What's the key for ...?

The four most convenient key bindings are:

*   Ctrl+Space: Content Assist
*   Ctrl+3: Quick Access -- gives you quick access to nearly everything (Eclipse 3.3+)
*   Ctrl+1: Quick Fix when there are problems, Quick Assist if not -- gives you quick means of fixing problems or making useful changes (Eclipse 3.3+)
*   Ctrl+Shift+L: Show common keyboard shortcuts (Eclipse 3.2+)

Here are a few others:

*   Ctrl+F6 / Ctrl+Shift+F6: Cycle editor windows forwards / backwards
*   Ctrl+F7 / Ctrl+Shift+F7: Cycle views forward / backwards
*   Ctrl+F8 / Ctrl+Shift+F8: Cycle perspectives forward / backwards
*   Ctrl+E: Show editor list / select editor window
*   F5: Refresh selected folder / file (useful if you edit files outside Eclipse)
*   Alt+Shift+X: Run As...
*   Alt+Shift+D: Debug As...
*   Alt+Shift+Q: Open View...

You can also remap key bindings via `Window > Preferences... > General > Keys` to suit your personal preference.

#### How do I add my own bindings?

See [Platform Plug-in Developer Guide > Programmer's Guide > Advanced workbench concepts > Workbench key bindings](http://help.eclipse.org/stable/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/wrkAdv_keyBindings_accelSet.htm).

#### Why can't I find the command I'm looking for?

*   To find commands which there are no keybindings:

    Window > Preferences... > General > Keys
      [x] Include unbound commands
    

 

*   To find other commands, if the above didn't work:

    Window > Preferences... > General > Keys > Advanced
      [ ] Filter action set contexts
      [ ] Filter internal contexts
      [ ] Filter uncategorized commands <-- this one is particularly useful


 

### Why did Content Assist stop working?

First, select:

    Window > Preferences... > General > Keys
    

 

Scroll to "Content Assist" and verify that Ctrl+Space is still the hotkey.

If content assist is displaying an empty proposal window, then check your default proposal generators by navigating to:

    Window > Preferences... > Java > Editor > Content Assist > Advanced
    

 

Ensure the top-most table (defining the default content assist list) has your desired proposal generators. You'll likely want "Java Proposals"

If Content Assist still doesn't work, several other things can be the cause:

*   Non-English software or keyboards
*   Accessibility software such as screen readers
*   Background processes with key bindings

One known process that can interfere with Ctrl+Space is Logitech's QuickCam10.exe. Upgrading to QuickCam 11 solves this problem. If you are not running this, try killing processes one by one until you get Ctrl+Space back.

### Why won't Content Assist work for my .xyz file type?

Make sure that you're opening the file with the correct editor. You may have several associated editors for a given file type, such as .php or .xml. Whatever was installed last is probably the default. If this is not your preferred editor, select:

    Window > Preferences... > General > Editors > File Associations
    

 

and set a better default. Note that the last editor you used for a file will be used next time, so you might have to use Open With instead of Open to select the correct editor.

If, for example, Content Assist works in your Java editor but not in your PHP editor, it could be a problem with your project's nature. See [How do I manually assign a project Nature or BuildCommand?](#How-do-I-manually-assign-a-project-Nature-or-BuildCommand.3F)


### How do I manually assign a project Nature or BuildCommand?

Many tools now add an option to "add the xyz nature", usually via the project's context menu. If present, use that. If not, here's another approach:

*   Create a new project of the type you need (such as PHP project or Java project or Plugin project)
*   Open the Navigator view
*   Open that new project's .project file.
*   Copy the <nature>s and <buildCommand>s from that .project into your actual project's .project file.

For a PHP project, this could be:

<buildSpec>
    <buildCommand>
        <name>org.eclipse.php.core.PhpIncrementalProjectBuilder</name>
        <arguments></arguments>
    </buildCommand>
    <buildCommand>
        <name>org.eclipse.wst.validation.validationbuilder</name>
        <arguments></arguments>
    </buildCommand>
</buildSpec>
<natures>
    <nature>org.eclipse.php.core.PHPNature</nature>
</natures>

For a Plug-in project:

<buildSpec>
    <buildCommand>
        <name>org.eclipse.jdt.core.javabuilder</name>
        <arguments></arguments>
    </buildCommand>
    <buildCommand>
        <name>org.eclipse.pde.ManifestBuilder</name>
        <arguments></arguments>
    </buildCommand>
    <buildCommand>
        <name>org.eclipse.pde.SchemaBuilder</name>
        <arguments></arguments>
    </buildCommand>
</buildSpec>
<natures>
    <nature>org.eclipse.pde.PluginNature</nature>
    <nature>org.eclipse.jdt.core.javanature</nature>
</natures>

Note also:

*   the order of the natures is important. See [bug 204883](https://bugs.eclipse.org/bugs/show_bug.cgi?id=204883).
*   some natures may conflict, such as PDT and phpeclipse. You might have to disable one nature to use the other.
*   restarting Eclipse should not be necessary, but if in doubt, try closing and reopening the project or restart it with Eclipse with [`eclipse -clean`](/Graphical_Eclipse_FAQs#I_have_just_installed_a_plug-in_but_I_do_not_see_any_indication_of_it_in_my_workspace._What_do_I_do.3F "Graphical Eclipse FAQs").

### How do I export a launch configuration?

Go into the 'Common' tab in your launch configuration and you will find a 'Browse' button to set the file that you want to export it as.

Starting from Eclipse 3.4, you can now export your launch configurations directly. Simply go File > Export... > Run/Debug > Launch Configurations.

### How do I find out which workspace I currently have open?

You can append the `-showLocation` to your Eclipse shortcut/script or `eclipse.ini` file. If you are going to edit the `eclipse.ini` file, you should make sure that you put it on a new line that's before the `-vmargs` line (if such a line exists). Once you have restarted Eclipse, you should be able to see the path to your workspace in the Eclipse instance's window's title bar.

### Why is Eclipse launching the current file I have open instead of whatever I last launched?

Eclipse 3.3 introduced a new feature named "Contextual Launching" which launches whatever you are currently working on or viewing. To get the old behaviour back wherein you always launch whatever you last launched, go to **Window** \> **Preferences** \> **Run/Debug** \> **Launching** and then under **Launch Operation** select **Always launch the previously launched application**.

### How do I configure Eclipse to use a black background with a white font?

For a consistent look, you have to use an operating system theme with a black background and white fonts. Alternatively, you can try setting the following preferences in eclipse:

*   Window > Preferences > General > Editors>Text Editors (foreground white, background black)

Note: Some editors specify their own colors, you may need to set the colors there as well.

Additionally, you will need to configure the syntax highlighting options for your editors. Here's how you do it for JDT:

1.  Window > Preferences > Java > Editor > Syntax Coloring
2.  For each rule that isn't enabled, enable it and set white as the color.
3.  For each rule that defines black as a color, set it to white.

### Where do I find the javadoc for the Eclipse API locally? I don't always want to load stuff up in a browser.

See [here](/Eclipse_Plug-in_Development_FAQ#Where_do_I_find_the_javadoc_for_the_Eclipse_API_locally.3F_I_don.27t_always_want_to_load_stuff_up_in_a_browser. "Eclipse Plug-in Development FAQ").

### Cut/Copy/Paste does not appear to be working properly on Linux. It's not often that I have to invoke the keyboard shortcut multiple times for it to take effect. What's the deal here?

You may be seeing an issue that's been logged to Eclipse as [bug 153809](https://bugs.eclipse.org/bugs/show_bug.cgi?id=153809) that appears to be caused by having [Klipper](http://www.raiden.net/?cat=2&aid=301) or [Glipper](http://glipper.sourceforge.net/) open. Please try disabling or closing the application and see if it resolves your problems.

### How do I show line numbers in the Eclipse text editor?

See [here](/Graphical_Eclipse_FAQs#How_do_I_show_line_numbers_in_the_Eclipse_text_editor.3F "Graphical Eclipse FAQs").

### How do I change the colour of the highlighting marker that highlights all the occurrences of some element in the text editor?

See [here](/Graphical_Eclipse_FAQs#How_do_I_change_the_colour_of_the_highlighting_marker_that_highlights_all_the_occurrences_of_some_element_in_the_text_editor.3F "Graphical Eclipse FAQs").

### How do I switch my workspace?

Access the 'File' menu and then select the 'Switch Workspace' menu item.

### I have just installed a plug-in but I do not see any indication of it in my workspace. What do I do?

See [here](/Graphical_Eclipse_FAQs#I_have_just_installed_a_plug-in_but_I_do_not_see_any_indication_of_it_in_my_workspace._What_do_I_do.3F "Graphical Eclipse FAQs").

### How do I check for the command line invocation that Eclipse used to launch an application?

For example, I'm running an Ant task in Eclipse and it works great, but outside Eclipse it won't run. How can I see how Eclipse is running it?

See [here](/Graphical_Eclipse_FAQs#How_do_I_check_for_the_command_line_invocation_that_Eclipse_used_to_launch_an_application.3F "Graphical Eclipse FAQs").

### Can projects exist outside of the workspace's folder?

Yes. Contrary to what many users are led to believe, projects can physically exist outside of the workspace's directory. When you try to create a new project, you should be able to change the location of the project (and not have it be created in the workspace). If this change is not possible, it would be a missing feature and it is recommended to log the issue against the offending Eclipse plug-in.

You can also import projects via 'File > Import > General > Import Existing Projects into the Workspace'. Be sure to uncheck the copy checkbox at the bottom after selecting the source folder.

### How do I change the list of workspaces listed under the 'Switch Workspace' submenu?

Starting in Eclipse 3.5, there is a preference page for this. See 'General > Startup and Shutdown > Workspaces'.

If you are in an older version of Eclipse, you can modify the `eclipse/configuration/.settings/org.eclipse.ui.ide.prefs` file by hand.

### How do I recover my saved passwords from the `.keyring` file?

The code snippet below should be able to help you. You may also wish to refer to [this page](http://waf-devel.blogspot.com/2009/07/eclipse-password-recovery-cvs.html).

The code below is largely copy and pasted from `org.eclipse.core.internal.runtime.auth.AuthorizationDatabase`, `org.eclipse.core.internal.runtime.auth.CipherInputStream`, and `org.eclipse.core.internal.runtime.auth.Cipher`.

		public static void main(String\[\] args) {
			String s = "/home/user/eclipse/configuration/org.eclipse.core.runtime/.keyring"; //$NON-NLS-1$
		 
			try {
				InputStream input = new FileInputStream(s);
				try {
					load(input);
				} finally {
					input.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		 
		private static final int MAGIC_NUMBER = 1;
		 
		private static void load(InputStream is) throws IOException,
				ClassNotFoundException {
			int version = is.read();
			if (version == MAGIC_NUMBER) {
				// read the authorization data
				CipherInputStream cis = new CipherInputStream(is, ""); //$NON-NLS-1$
				ObjectInputStream ois = new ObjectInputStream(cis);
				try {
					Map authorizationInfo = (Hashtable) ois.readObject();
					System.out.println(authorizationInfo);
				} finally {
					ois.close();
				}
			}
		}
		 
		static class CipherInputStream extends FilterInputStream {
			private static final int SKIP\_BUFFER\_SIZE = 2048;
			private Cipher cipher;
		 
			public CipherInputStream(InputStream is, String password) {
				super(is);
				cipher = new Cipher(Cipher.DECRYPT_MODE, password);
			}
		 
			public boolean markSupported() {
				return false;
			}
		 
			public int read() throws IOException {
				int b = super.read();
				if (b == -1)
					return -1;
				try {
					return (cipher.cipher((byte) b)) & 0x00ff;
				} catch (Exception e) {
					throw new IOException(e.getMessage());
				}
			}
		 
			public int read(byte b\[\], int off, int len) throws IOException {
				int bytesRead = in.read(b, off, len);
				if (bytesRead == -1)
					return -1;
				try {
					byte\[\] result = cipher.cipher(b, off, bytesRead);
					for (int i = 0; i < result.length; ++i)
						b\[i + off\] = result\[i\];
					return bytesRead;
				} catch (Exception e) {
					throw new IOException(e.getMessage());
				}
			}
		 
			public long skip(long n) throws IOException {
				byte\[\] buffer = new byte\[SKIP\_BUFFER\_SIZE\];
		 
				int bytesRead = 0;
				long bytesRemaining = n;
		 
				while (bytesRead != -1 && bytesRemaining > 0) {
					bytesRead = read(buffer, 0, (int) Math.min(SKIP\_BUFFER\_SIZE,
							bytesRemaining));
					if (bytesRead > 0) {
						bytesRemaining -= bytesRead;
					}
				}
		 
				return n - bytesRemaining;
			}
		}
		 
		static class Cipher {
			public static final int DECRYPT_MODE = -1;
			public static final int ENCRYPT_MODE = 1;
			private static final int RANDOM_SIZE = 16;
		 
			private int mode = 0;
			private byte\[\] password = null;
		 
			private byte\[\] byteStream;
			private int byteStreamOffset;
			private MessageDigest digest;
			private Random random;
			private final byte\[\] toDigest;
		 
			public Cipher(int mode, String passwordString) {
				this.mode = mode;
				try {
					this.password = passwordString.getBytes("UTF8"); //$NON-NLS-1$
				} catch (UnsupportedEncodingException e) {
					this.password = passwordString.getBytes();
				}
				toDigest = new byte\[password.length + RANDOM_SIZE\];
			}
		 
			public byte\[\] cipher(byte\[\] data) throws Exception {
				return transform(data, 0, data.length, mode);
			}
		 
			public byte\[\] cipher(byte\[\] data, int off, int len) throws Exception {
				return transform(data, off, len, mode);
			}
		 
			public byte cipher(byte datum) throws Exception {
				byte\[\] data = { datum };
				return cipher(data)\[0\];
			}
		 
			private byte\[\] generateBytes() throws Exception {
				if (digest == null) {
					digest = MessageDigest.getInstance("SHA"); //$NON-NLS-1$
					// also seed random number generator based on password
					long seed = 0;
					for (int i = 0; i < password.length; i++)
						// this function is known to give good hash distribution for
						// character data
						seed = (seed * 37) + password\[i\];
					random = new Random(seed);
				}
				// add random bytes to digest array
				random.nextBytes(toDigest);
		 
				// overlay password onto digest array
				System.arraycopy(password, 0, toDigest, 0, password.length);
		 
				// compute and return SHA-1 hash of digest array
				return digest.digest(toDigest);
			}
		 
			private byte\[\] nextRandom(int length) throws Exception {
				byte\[\] nextRandom = new byte\[length\];
				int nextRandomOffset = 0;
				while (nextRandomOffset < length) {
					if (byteStream == null || byteStreamOffset >= byteStream.length) {
						byteStream = generateBytes();
						byteStreamOffset = 0;
					}
					nextRandom\[nextRandomOffset++\] = byteStream\[byteStreamOffset++\];
				}
				return nextRandom;
			}
		 
			private byte\[\] transform(byte\[\] data, int off, int len, int mod)
					throws Exception {
				byte\[\] result = nextRandom(len);
				for (int i = 0; i < len; ++i) {
					result\[i\] = (byte) (data\[i + off\] + mod * result\[i\]);
				}
				return result;
			}
		}

### My line delimiter changes are not being persisted to the file. What's going on?

See [here](http://www-01.ibm.com/support/docview.wss?rs=79&context=SSJNRR&context=SSNJU5&context=SSSTY3&uid=swg21315284&loc=en_US&cs=UTF-8&lang=en).

### Black background color for tooltips on Linux/Ubuntu/GTK

Use Eclipse 3.6 or higher, this was caused by a bug in SWT ([bug 309907](https://bugs.eclipse.org/bugs/show_bug.cgi?id=309907)). If it still happens check your theme settings. On Ubuntu 10.04, the default color scheme of the 'Radiance' theme for tooltips is white text on black background (see System > Preferences > Appearance > Theme > Colours > Tooltips). [Here](https://bugs.launchpad.net/ubuntu/+source/light-themes/+bug/540332) is the bug on the matter on Ubuntu's Launchpad.

In Ubuntu 11.10 and 12.04 there is no interface to change the color scheme of the theme, so it may be useful to edit the gtkrc file in '/usr/share/themes/<yourtheme>/gtk-2.0/' to set the tooltip background and foreground colors. E.g. 'tooltip\_fg\_color:#000000' & 'tooltip\_bg\_color:#E6E6FA'.

You can also install and open **gnome-color-chooser**: Go to **Specific > Tooltips** and put black foreground over pale yellow background.

### Excessive tab folder height on Linux/Ubuntu/GTK

Tab folder height is calculated by the height of toolbars which can have padding in GTK themes. To fix this, edit '/usr/share/themes/<yourtheme>/gtk-2.0/gtkrc'. Look for:

GtkToolbar::internal-padding = 1

### How can I easily migrate settings and preferences between my Eclipse workspaces?

You may want to take a look at [Workspace Mechanic for Eclipse](http://code.google.com/a/eclipselabs.org/p/workspacemechanic/).

### How do I swap between different programs' output in the 'Console' view? How do I open another 'Console' view?

You can [switch between active consoles](http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.jdt.doc.user/reference/views/console/ref-display_action.htm) and [create new 'Console' views](http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.jdt.doc.user/reference/views/console/ref-open_action.htm) using the items in the view's toolbar.

Java Development Tools (JDT)
----------------------------

### The javadoc for the standard Java classes does not show up as context help. What is the problem? Should I download the javadocs?

To get the standard Java javadoc to display in hover and context help in the Eclipse Java Editor, you need to run a JDK . Eclipse retrieves the javadoc from the JDK Java sources. The sources are bundled with a JDK but not with a JRE. The file containing the sources in the SUN JDK is src.zip.

#### What do you mean by 'run a JDK'?

**The problem is that I've got unpacked java docs (and in archive too) at the proper location in the JDK dir and it is not displayed when working on java project...moreover javadoc specific to project is shown properly!**

You need either to have the JDK set as the Java Runtime for your project or workspace, or have started Eclipse with that JDK. Make sure that the root dir of your JDK installation contains a file called src.zip.

#### But it still does \*not\* work! Help me!

Make sure that you have selected the JDK in your workspace or project preferences. For the workspace check under Windows -> Preferences -> Java -> Installed JREs. For a project, check the project's properties. Also remember to check any launch configurations under Run -> Run Configurations...

#### But I'm on MacOS X which comes with the JDK

The Java installation that comes with MacOS X (10.6 and prior) or is installed (10.7 and later) does not include the source bundle. To see the JDK source, you need to first install the Apple-supplied Java Developer Update for your OS from [Apple's Developer Site](http://developer.apple.com/java) (requires an Apple ID). In 10.7 and later (and perhaps 10.6 too?), these JDKs are installed in /Library/Java/JavaVirtualMachines as _{jvm-version}.jdk_.

(Another alternative, not described here, is to install the [OpenJDK for MacOS X](http://openjdk.java.net/projects/macosx-port/).)

Having installed the JDK, you have two options:

1.  The first option is to configure Eclipse to add your newly-installed JDK to as a separate JDKs.
    1.  Open _Preferences → Java → Installed JREs_ and select _Add... → MacOS X VM_
    2.  For JRE home, add _/Library/Java/JavaVirtualMachines/{jvm-version}.jdk/Contents/Home_ where _{jvm-version}}.jdk_ is the directory corresponding to your newly-installed JVM. Note that the _Contents/Home_ is essential.
    3.  Select Finish to return to the _Installed JREs_ dialog.
    4.  Tick your newly added JDK to make it the default JRE. This will likely trigger a rebuild.
2.  The second option is to configure your existing JRE to fetch source from the src.jar included in your newly-installed JDK. Note that the debugger may not show the exact source location.
    1.  Open _Preferences → Java → Installed JREs_, select the default JRE, and then click on the _Edit..._ button.
    2.  In the "JRE system libraries" section, select the "classes.jar". If you expand the arrow, it should show _Source attachment: (none)_
    3.  Click on the _Source Attachment..._ button on the right
    4.  In the location path, specify _/Library/Java/JavaVirtualMachines/{jvm-version}.jdk/Contents/Home/src.jar_
    5.  You may need to repeat the above for any other jar that is installed as part of this JRE. Note that such jars may only be shipped in binary form, with no source available.
    6.  Select _Finish_, and then _OK_ in the _Installed JREs_ preferences dialog.

You should now be able to find the source for classes from your JRE.

Sources: [Stack Overflow](http://stackoverflow.com/a/4193828)

### How do I override the environment variables that Ant uses during execution?

To override environment variables passed to Ant, open your launch configuration.

*   On the 'JRE' tab choose 'Separate JRE'. Select the required JRE from the list.
*   On the 'Environment' tab, click 'Select' button then pick the variables you want to override from the list and click OK. Click 'Edit' to change values.

### Why is Content Assist not working in the Java editor? Why doesn't Eclipse recognize my .java file as a Java file?

Please try the following steps:

*   Window > Preferences... (for Mac users: Eclipse > Preferences...)
*   Navigate to Java > Editor > Content Assist > Advanced
*   Ensure that all the entries in the uppermost list are checked.
*   Click 'Okay'.

Now check whether content assist is working again.

Note that for Eclipse to treat a .java file as a Java file with full syntax highlighting and code completion, it must be in a Java or Plug-in project, and located in a properly-defined source folder. Right-click your project's root folder and select properties to add more source folders if necessary, or move your file into the src/ tree.

Try creating a new Java project, then pasting your file into the src/ folder tree; or, try a new workspace (File > Switch Workspace... on a non-existent folder).

See also [Manually assigning a projectNature or BuildCommand](/IRC_FAQ#How_do_I_manually_assign_a_project_Nature_or_BuildCommand.3F "IRC FAQ").

### How do I change the Java compiler compliance level for my workspace?

See [here](/Graphical_Eclipse_FAQs#How_do_I_change_the_compiler_compliance_level_for_my_workspace.3F "Graphical Eclipse FAQs").

### How do I add arguments to the Java program I am running?

See [here](/Graphical_Eclipse_FAQs#How_do_I_add_arguments_to_the_Java_program_I_am_running.3F "Graphical Eclipse FAQs").

### How do I alter my package representation so that parent packages are housing child packages?

See [here](/Graphical_Eclipse_FAQs#How_do_I_alter_my_package_representation_so_that_parent_packages_are_housing_child_packages.3F "Graphical Eclipse FAQs").

### I clicked on something and now I can only see the method that I am currently editing. What do I do? Did I lose my entire file?

See [here](/Graphical_Eclipse_FAQs#I_clicked_on_something_and_now_I_can_only_see_the_method_that_I_am_currently_editing._What_do_I_do.3F_Did_I_lose_my_entire_file.3F "Graphical Eclipse FAQs").

### I would like code completion to be activated as I type like how it works in Visual Studio? Can I turn this on somewhere?

No, this is currently not implemented. Please refer to [bug 101420](https://bugs.eclipse.org/bugs/show_bug.cgi?id=101420) "\[content assist\] auto-activation everywhere". You may also want to look at [bug 159157](https://bugs.eclipse.org/bugs/show_bug.cgi?id=159157), specifically [bug 159157 comment 12](https://bugs.eclipse.org/bugs/show_bug.cgi?id=159157#c12) for a work-around.

### I've been told that Eclipse has its own Java compiler, is this true? Can I use Sun's javac instead?

Yes, Eclipse's JDT project has its own compiler, named ECJ (Eclipse Compiler for Java). ECJ is the compiler that will be used when you save or invoke builds from within Eclipse. If you wish to use the javac compiler instead, you will have to use Apache Ant instead. On a slightly related topic, it is also possible to have Ant build with ECJ instead of with javac.

### I call [System.console()](http://java.sun.com/javase/6/docs/api/java/lang/System.html#console%28%29) in my code but null is returned. It does work in the command line though. What's going on?

Please see [bug 122429](https://bugs.eclipse.org/bugs/show_bug.cgi?id=122429).

### Why isn't my { class | jdbc driver | ... } being found?

**If you are creating OSGi bundles or Eclipse plug-ins, skip this section**

Type resolution errors, JDBC driver errors, or ClassNotFoundException exceptions at runtime indicate that your project's classpath missing some required dependencies. JDT requires that you configure your project's classpath to reference any jar files required by your code.

1.  Right-click on your Java project
2.  Select _Properties_ to open the project properties dialog
3.  Select the Java Build Path item in the navigation tree
4.  Select the Libraries tab
5.  Add your jars

The generally recommended approach is to include necessary jar files your project, or a related project, and then use _Add JARs..._ to select the appropriate jar files.

PHP Development Tools (PDT)
---------------------------

### When I try to create PHP project, I get an error saying "Creation of element failed.", what should I do?

See [bug 280935](https://bugs.eclipse.org/bugs/show_bug.cgi?id=280935) for more details.

Plug-in Development
-------------------

### How do I test my plug-ins?

See [here](/PDE./FAQ#How_do_test_my_plug-ins.md "PDE./FAQ")..md 
### I get an unhandled event loop exception in my console. What gives?

See [here](/Graphical_Eclipse_FAQs#I_get_an_unhandled_event_loop_exception_in_my_console._What_gives.3F "Graphical Eclipse FAQs").

### A plug-in in my 'Eclipse Application' launch configuration is listed as being "out of sync", what should I do?

See [here](/Eclipse_Plug-in_Development_FAQ#A_plug-in_in_my_.27Eclipse_Application.27_launch_configuration_is_listed_as_being_.22out_of_sync.22.2C_what_should_I_do.3F "Eclipse Plug-in Development FAQ").

### I added a jar to my classpath, but it's not being found! What should I do?

You cannot simply add jar files to plugin projects (aka bundles in OSGi terminology) as you would with normal Java projects. Plugin projects must instead declare their dependencies in their MANIFEST.MF. The Plugin Development Environment (PDE) provides tooling for managing these dependencies file, and then transforms the plugin's MANIFEST.MF into a classpath that JDT can work with. But that JDT classpath is only used for editing within Eclipse; PDE applies another transformation to set up the classpath used for a runtime workspace.

For suggestions on adding a jar, see [here](/Eclipse_Plug-in_Development_FAQ#I.27m_using_third_party_jar_files_and_my_plug-in_is_not_working... "Eclipse Plug-in Development FAQ").

### How do I find the source for a plugin?

Plugins are automatically hooked up into the Java Search by JDT when they are referenced as a dependency of a project open in a workspace. Many plugins have [corresponding source bundles](http://help.eclipse.org/indigo/index.jsp?topic=/org.eclipse.pde.doc.user/tasks/pde_individual_source.htm) that are usually included as part of an "SDK" feature. Often the simplest approach is then:

1.  Ensure you have installed the appropriate SDK feature with _Help > Install New Software_
2.  Add the required plugins as a dependency of one of your projects.

If you wish to examine the source for a plugin that is not a dependency, then you need to explicitly add those plugins to the Java Search path:

1.  Open the _Plug-ins_ view
2.  Select the appropriate plugins, right-click and select _Add to Java Search_

The contents of those plugins should now be available through _Java Search_, _Open Type..._ and other JDT facilities.

### How do I edit the source for a plugin?

If you wish to be able to edit the source code for a plugin, you may be able to simply import the source bundle as a proper project within your workspace.

1.  Open the "Plug-ins" view.
2.  Select the appropriate plugins, right-click and select "Import As > Source Project" menu to bring in those plugins into your workspace.

Many plugins now ship with [appropriate metadata](http://help.eclipse.org/indigo/index.jsp?topic=%2Forg.eclipse.pde.doc.user%2Ftasks%2Fui_import_from_cvs.htm) to actually check out the source code from the corresponding version control system. In this case you can use _Import As > Project from a Repository..._.

Otherwise it will be necessary to visit the [corresponding project's page](http://www.eclipse.org/projects/listofprojects.php) and look for its development information to find out how to access the project's source repositories. Many Eclipse projects have now switched to Git, and those repositories are listed in an [easily searchable form](http://git.eclipse.org).

SWT
---

### I cannot get the SWT widget ABC to work when I do XYZ. Could you help me?

Check the [SWT Snippets](https://www.eclipse.org/swt/snippets/) section, there might be a code example that demonstrates what you are trying to do.here.

