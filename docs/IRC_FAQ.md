

IRC FAQ
=======

A collection of FAQs gathered in the Eclipse [IRC](/IRC "IRC") channels. Some of them are logged, see the specific channel for details.

Contents
--------

*   [1 General](#General)
    *   [1.1 I'm new, what should I read first?](#I.27m-new.2C-what-should-I-read-first.3F)
    *   [1.2 What are #eclipse and #eclipse-dev about?](#What-are-.23eclipse-and-.23eclipse-dev-about.3F)
    *   [1.3 I am looking for help with developing with Eclipse. Should I ask on #eclipse or #eclipse-dev?](#I-am-looking-for-help-with-developing-with-Eclipse.-Should-I-ask-on-.23eclipse-or-.23eclipse-dev.3F)
    *   [1.4 Are there other active channels about Eclipse?](#Are-there-other-active-channels-about-Eclipse.3F)
    *   [1.5 Nobody is answering my question. What should I do?](#Nobody-is-answering-my-question.-What-should-I-do.3F)
*   [2 Installation, Startup and Runtime](#Installation.2C-Startup-and-Runtime)
    *   [2.1 Where can I get Eclipse?](#Where-can-I-get-Eclipse.3F)
    *   [2.2 What's the difference between all the different packages like 'Eclipse IDE for Java Developers' and the 'Eclipse IDE for Java EE Developers'? What do they contain? Do they contain source code?](#What.27s-the-difference-between-all-the-different-packages-like-.27Eclipse-IDE-for-Java-Developers.27-and-the-.27Eclipse-IDE-for-Java-EE-Developers.27.3F-What-do-they-contain.3F-Do-they-contain-source-code.3F)
    *   [2.3 There is no package for the platform I'm on. What can I do?](#There-is-no-package-for-the-platform-I.27m-on.-What-can-I-do.3F)
    *   [2.4 I don't want multiple Eclipse installations. How do I merge those packages into one?](#I-don.27t-want-multiple-Eclipse-installations.-How-do-I-merge-those-packages-into-one.3F)
    *   [2.5 How do I verify my download? Are there any MD5 or SHA1 hashes for me to verify my download against?](#How-do-I-verify-my-download.3F-Are-there-any-MD5-or-SHA1-hashes-for-me-to-verify-my-download-against.3F)
    *   [2.6 What are all these strangely named releases?](#What-are-all-these-strangely-named-releases.3F)
    *   [2.7 Where can I get a list of all the Eclipse projects?](#Where-can-I-get-a-list-of-all-the-Eclipse-projects.3F)
    *   [2.8 Where can I get project XYZ?](#Where-can-I-get-project-XYZ.3F)
        *   [2.8.1 Is there a GUI Builder?](#Is-there-a-GUI-Builder.3F)
        *   [2.8.2 Is there a PHP editor for Eclipse 3.3 Europa / Eclipse 3.4 Ganymede?](#Is-there-a-PHP-editor-for-Eclipse-3.3-Europa-.2F-Eclipse-3.4-Ganymede.3F)
    *   [2.9 What is p2?](#What-is-p2.3F)
    *   [2.10 What is the Update Manager?](#What-is-the-Update-Manager.3F)
    *   [2.11 How do I start Eclipse?](#How-do-I-start-Eclipse.3F)
    *   [2.12 How do I upgrade/update Eclipse?](#How-do-I-upgrade.2Fupdate-Eclipse.3F)
    *   [2.13 What other command line arguments are available?](#What-other-command-line-arguments-are-available.3F)
    *   [2.14 How do I debug Eclipse? How can I see what plug-ins are being started? Why aren't the plug-ins I installed showing up in the UI? How do I start the OSGi console?](#How-do-I-debug-Eclipse.3F-How-can-I-see-what-plug-ins-are-being-started.3F-Why-aren.27t-the-plug-ins-I-installed-showing-up-in-the-UI.3F-How-do-I-start-the-OSGi-console.3F)
        *   [2.14.1 Debugging OSGi Bundle Loading Issues](#Debugging-OSGi-Bundle-Loading-Issues)
        *   [2.14.2 Debugging Eclipse Using Eclipse](#Debugging-Eclipse-Using-Eclipse)
            *   [2.14.2.1 Launching as an Eclipse Application using PDE](#Launching-as-an-Eclipse-Application-using-PDE)
            *   [2.14.2.2 Attaching to a running instance](#Attaching-to-a-running-instance)
        *   [2.14.3 Shared Installation Problems](#Shared-Installation-Problems)
    *   [2.15 How do I find out which Java runtime environment is being used to run Eclipse?](#How-do-I-find-out-which-Java-runtime-environment-is-being-used-to-run-Eclipse.3F)
    *   [2.16 Can I upgrade from Eclipse 3.2 (Callisto) to Eclipse 3.3 (Europa)?](#Can-I-upgrade-from-Eclipse-3.2-.28Callisto.29-to-Eclipse-3.3-.28Europa.29.3F)
    *   [2.17 Can I use my Eclipse workspace from an old release with a new Eclipse release?](#Can-I-use-my-Eclipse-workspace-from-an-old-release-with-a-new-Eclipse-release.3F)
    *   [2.18 How do I use a different workspace?](#How-do-I-use-a-different-workspace.3F)
    *   [2.19 How do I install Eclipse on Linux?](#How-do-I-install-Eclipse-on-Linux.3F)
    *   [2.20 How do I install Eclipse on Solaris?](#How-do-I-install-Eclipse-on-Solaris.3F)
    *   [2.21 I just unzipped Eclipse, but it does not start. Why?](#I-just-unzipped-Eclipse.2C-but-it-does-not-start.-Why.3F)
    *   [2.22 I just installed Eclipse on Linux, but it does not start. What is the problem?](#I-just-installed-Eclipse-on-Linux.2C-but-it-does-not-start.-What-is-the-problem.3F)
    *   [2.23 I just installed Eclipse on my 64-bit system, but it does not start. What is the problem?](#I-just-installed-Eclipse-on-my-64-bit-system.2C-but-it-does-not-start.-What-is-the-problem.3F)
    *   [2.24 When I try to install a plug-in, I get a "Cannot connect to keystore." error, what should I do?](#When-I-try-to-install-a-plug-in.2C-I-get-a-.22Cannot-connect-to-keystore..22-error.2C-what-should-I-do.3F)
    *   [2.25 When I start Eclipse it says "Workspace in use or cannot be created, choose a different one.", what should I do?](#When-I-start-Eclipse-it-says-.22Workspace-in-use-or-cannot-be-created.2C-choose-a-different-one..22.2C-what-should-I-do.3F)
    *   [2.26 How do I copy plugins between Eclipse installations with p2?](#How-do-I-copy-plugins-between-Eclipse-installations-with-p2.3F)
    *   [2.27 How come my list of update sites is completely empty when other people says theirs has stuff in it?](#How-come-my-list-of-update-sites-is-completely-empty-when-other-people-says-theirs-has-stuff-in-it.3F)
    *   [2.28 How do I install PDT?](#How-do-I-install-PDT.3F)
    *   [2.29 How do I install a plug-in with multiple dependencies?](#How-do-I-install-a-plug-in-with-multiple-dependencies.3F)
    *   [2.30 How do I uninstall a plug-in?](#How-do-I-uninstall-a-plug-in.3F)
    *   [2.31 I'm getting "Network is unreachable" error messages when I'm trying to use the provisioning system on a Debian/Debian-based system. What should I do?](#I.27m-getting-.22Network-is-unreachable.22-error-messages-when-I.27m-trying-to-use-the-provisioning-system-on-a-Debian.2FDebian-based-system.-What-should-I-do.3F)
*   [3 Crashers, Freezing, and other Major Issues](#Crashers.2C-Freezing.2C-and-other-Major-Issues)
    *   [3.1 Eclipse is constantly crashing for me on Oracle's / Apple's Java 6 HotSpot VM...](#Eclipse-is-constantly-crashing-for-me-on-Oracle.27s-.2F-Apple.27s-Java-6-HotSpot-VM...)
    *   [3.2 Eclipse gets past the splash screen but then an empty window appears / Eclipse is crashing on me whenever I initiate a browser component such as hovering over Java methods for javadoc tooltips...](#Eclipse-gets-past-the-splash-screen-but-then-an-empty-window-appears-.2F-Eclipse-is-crashing-on-me-whenever-I-initiate-a-browser-component-such-as-hovering-over-Java-methods-for-javadoc-tooltips...)
    *   [3.3 The Java EE build for Eclipse Ganymede / Eclipse 3.4 does not run for me but the Eclipse Classic build starts up just fine...](#The-Java-EE-build-for-Eclipse-Ganymede-.2F-Eclipse-3.4-does-not-run-for-me-but-the-Eclipse-Classic-build-starts-up-just-fine...)
    *   [3.4 Why does Eclipse hang for an extended period of time after opening an editor in Linux/gtk+?](#Why-does-Eclipse-hang-for-an-extended-period-of-time-after-opening-an-editor-in-Linux.2Fgtk.2B.3F)
    *   [3.5 Eclipse is crashing for me when performing Subversion operations on Windows. I am using Subclipse.](#Eclipse-is-crashing-for-me-when-performing-Subversion-operations-on-Windows.-I-am-using-Subclipse.)
    *   [3.6 I'm having memory, heap, or permgen problems, what can I do?](#I.27m-having-memory.2C-heap.2C-or-permgen-problems.2C-what-can-I-do.3F)
    *   [3.7 Eclipse is constantly crashing for me on Linux and I have a lot of plug-ins installed. There is a mentioning of "too many open files".](#Eclipse-is-constantly-crashing-for-me-on-Linux-and-I-have-a-lot-of-plug-ins-installed.-There-is-a-mentioning-of-.22too-many-open-files.22.)
    *   [3.8 Eclipse buttons in dialogs and other places are not working for me if I click them with the mouse. I also cannot see anything in the tree when I try to install updates. What's going on?](#Eclipse-buttons-in-dialogs-and-other-places-are-not-working-for-me-if-I-click-them-with-the-mouse.-I-also-cannot-see-anything-in-the-tree-when-I-try-to-install-updates.-What.27s-going-on.3F)
    *   [3.9 Eclipse seems to be hanging on startup. How can I find out why?](#Eclipse-seems-to-be-hanging-on-startup.-How-can-I-find-out-why.3F)
    *   [3.10 Update complains that it cannot find a repository](#Update-complains-that-it-cannot-find-a-repository)
    *   [3.11 Eclipse keeps running out of (permgen) memory when I use Oracle/Sun's Java 6 update 21 on Windows](#Eclipse-keeps-running-out-of-.28permgen.29-memory-when-I-use-Oracle.2FSun.27s-Java-6-update-21-on-Windows)
    *   [3.12 After startup, i see only an empty Dialog - eclipse won't start](#After-startup.2C-i-see-only-an-empty-Dialog---eclipse-won.27t-start)
*   [4 Eclipse](#Eclipse)
    *   [4.1 How do I create a project for an existing source directory?](#How-do-I-create-a-project-for-an-existing-source-directory.3F)
        *   [4.1.1 Option 1: Import the source into an existing project](#Option-1:-Import-the-source-into-an-existing-project)
        *   [4.1.2 Option 2: Create project on the existing source directory](#Option-2:-Create-project-on-the-existing-source-directory)
        *   [4.1.3 Option 3: Create project and link to existing source](#Option-3:-Create-project-and-link-to-existing-source)
    *   [4.2 Where are Eclipse's log files located?](#Where-are-Eclipse.27s-log-files-located.3F)
    *   [4.3 I was working on a project and doing something or other does not work. Where should I start?](#I-was-working-on-a-project-and-doing-something-or-other-does-not-work.-Where-should-I-start.3F)
    *   [4.4 Where are Eclipse preferences stored?](#Where-are-Eclipse-preferences-stored.3F)
    *   [4.5 Where are update site bookmarks stored?](#Where-are-update-site-bookmarks-stored.3F)
    *   [4.6 Where are my Eclipse plug-ins folder?](#Where-are-my-Eclipse-plug-ins-folder.3F)
    *   [4.7 What's the key for ...?](#What.27s-the-key-for-....3F)
        *   [4.7.1 How do I add my own bindings?](#How-do-I-add-my-own-bindings.3F)
        *   [4.7.2 Why can't I find the command I'm looking for?](#Why-can.27t-I-find-the-command-I.27m-looking-for.3F)
    *   [4.8 Why did Content Assist stop working?](#Why-did-Content-Assist-stop-working.3F)
    *   [4.9 Why won't Content Assist work for my .xyz file type?](#Why-won.27t-Content-Assist-work-for-my-.xyz-file-type.3F)
    *   [4.10 How to have a custom .php extension recognized by PDT (or .xml by the XML editor and other XML features, etc.)?](#How-to-have-a-custom-.php-extension-recognized-by-PDT-.28or-.xml-by-the-XML-editor-and-other-XML-features.2C-etc..29.3F)
    *   [4.11 How do I manually assign a project Nature or BuildCommand?](#How-do-I-manually-assign-a-project-Nature-or-BuildCommand.3F)
    *   [4.12 How do I export a launch configuration?](#How-do-I-export-a-launch-configuration.3F)
    *   [4.13 How do I find out which workspace I currently have open?](#How-do-I-find-out-which-workspace-I-currently-have-open.3F)
    *   [4.14 Why is Eclipse launching the current file I have open instead of whatever I last launched?](#Why-is-Eclipse-launching-the-current-file-I-have-open-instead-of-whatever-I-last-launched.3F)
    *   [4.15 How do I configure Eclipse to use a black background with a white font?](#How-do-I-configure-Eclipse-to-use-a-black-background-with-a-white-font.3F)
    *   [4.16 Where do I find the javadoc for the Eclipse API locally? I don't always want to load stuff up in a browser.](#Where-do-I-find-the-javadoc-for-the-Eclipse-API-locally.3F-I-don.27t-always-want-to-load-stuff-up-in-a-browser.)
    *   [4.17 Cut/Copy/Paste does not appear to be working properly on Linux. It's not often that I have to invoke the keyboard shortcut multiple times for it to take effect. What's the deal here?](#Cut.2FCopy.2FPaste-does-not-appear-to-be-working-properly-on-Linux.-It.27s-not-often-that-I-have-to-invoke-the-keyboard-shortcut-multiple-times-for-it-to-take-effect.-What.27s-the-deal-here.3F)
    *   [4.18 How do I show line numbers in the Eclipse text editor?](#How-do-I-show-line-numbers-in-the-Eclipse-text-editor.3F)
    *   [4.19 How do I change the colour of the highlighting marker that highlights all the occurrences of some element in the text editor?](#How-do-I-change-the-colour-of-the-highlighting-marker-that-highlights-all-the-occurrences-of-some-element-in-the-text-editor.3F)
    *   [4.20 How do I switch my workspace?](#How-do-I-switch-my-workspace.3F)
    *   [4.21 I have just installed a plug-in but I do not see any indication of it in my workspace. What do I do?](#I-have-just-installed-a-plug-in-but-I-do-not-see-any-indication-of-it-in-my-workspace.-What-do-I-do.3F)
    *   [4.22 How do I check for the command line invocation that Eclipse used to launch an application?](#How-do-I-check-for-the-command-line-invocation-that-Eclipse-used-to-launch-an-application.3F)
    *   [4.23 Can projects exist outside of the workspace's folder?](#Can-projects-exist-outside-of-the-workspace.27s-folder.3F)
    *   [4.24 How do I change the list of workspaces listed under the 'Switch Workspace' submenu?](#How-do-I-change-the-list-of-workspaces-listed-under-the-.27Switch-Workspace.27-submenu.3F)
    *   [4.25 How do I recover my saved passwords from the .keyring file?](#How-do-I-recover-my-saved-passwords-from-the-.keyring-file.3F)
    *   [4.26 My line delimiter changes are not being persisted to the file. What's going on?](#My-line-delimiter-changes-are-not-being-persisted-to-the-file.-What.27s-going-on.3F)
    *   [4.27 Black background color for tooltips on Linux/Ubuntu/GTK](#Black-background-color-for-tooltips-on-Linux.2FUbuntu.2FGTK)
    *   [4.28 Excessive tab folder height on Linux/Ubuntu/GTK](#Excessive-tab-folder-height-on-Linux.2FUbuntu.2FGTK)
    *   [4.29 How can I easily migrate settings and preferences between my Eclipse workspaces?](#How-can-I-easily-migrate-settings-and-preferences-between-my-Eclipse-workspaces.3F)
    *   [4.30 How do I swap between different programs' output in the 'Console' view? How do I open another 'Console' view?](#How-do-I-swap-between-different-programs.27-output-in-the-.27Console.27-view.3F-How-do-I-open-another-.27Console.27-view.3F)
*   [5 Java Development Tools (JDT)](#Java-Development-Tools-.28JDT.29)
    *   [5.1 The javadoc for the standard Java classes does not show up as context help. What is the problem? Should I download the javadocs?](#The-javadoc-for-the-standard-Java-classes-does-not-show-up-as-context-help.-What-is-the-problem.3F-Should-I-download-the-javadocs.3F)
        *   [5.1.1 What do you mean by 'run a JDK'?](#What-do-you-mean-by-.27run-a-JDK.27.3F)
        *   [5.1.2 But it still does \*not\* work! Help me!](#But-it-still-does-.2Anot.2A-work.21-Help-me.21)
        *   [5.1.3 But I'm on MacOS X which comes with the JDK](#But-I.27m-on-MacOS-X-which-comes-with-the-JDK)
    *   [5.2 How do I override the environment variables that Ant uses during execution?](#How-do-I-override-the-environment-variables-that-Ant-uses-during-execution.3F)
    *   [5.3 Why is Content Assist not working in the Java editor? Why doesn't Eclipse recognize my .java file as a Java file?](#Why-is-Content-Assist-not-working-in-the-Java-editor.3F-Why-doesn.27t-Eclipse-recognize-my-.java-file-as-a-Java-file.3F)
    *   [5.4 How do I change the Java compiler compliance level for my workspace?](#How-do-I-change-the-Java-compiler-compliance-level-for-my-workspace.3F)
    *   [5.5 How do I add arguments to the Java program I am running?](#How-do-I-add-arguments-to-the-Java-program-I-am-running.3F)
    *   [5.6 How do I alter my package representation so that parent packages are housing child packages?](#How-do-I-alter-my-package-representation-so-that-parent-packages-are-housing-child-packages.3F)
    *   [5.7 I clicked on something and now I can only see the method that I am currently editing. What do I do? Did I lose my entire file?](#I-clicked-on-something-and-now-I-can-only-see-the-method-that-I-am-currently-editing.-What-do-I-do.3F-Did-I-lose-my-entire-file.3F)
    *   [5.8 I would like code completion to be activated as I type like how it works in Visual Studio? Can I turn this on somewhere?](#I-would-like-code-completion-to-be-activated-as-I-type-like-how-it-works-in-Visual-Studio.3F-Can-I-turn-this-on-somewhere.3F)
    *   [5.9 I've been told that Eclipse has its own Java compiler, is this true? Can I use Sun's javac instead?](#I.27ve-been-told-that-Eclipse-has-its-own-Java-compiler.2C-is-this-true.3F-Can-I-use-Sun.27s-javac-instead.3F)
    *   [5.10 I call System.console() in my code but null is returned. It does work in the command line though. What's going on?](#I-call-System.console.28.29-in-my-code-but-null-is-returned.-It-does-work-in-the-command-line-though.-What.27s-going-on.3F)
    *   [5.11 Why isn't my { class | jdbc driver | ... } being found?](#Why-isn.27t-my-.7B-class-.7C-jdbc-driver-.7C-...-.7D-being-found.3F)
*   [6 PHP Development Tools (PDT)](#PHP-Development-Tools-.28PDT.29)
    *   [6.1 When I try to create PHP project, I get an error saying "Creation of element failed.", what should I do?](#When-I-try-to-create-PHP-project.2C-I-get-an-error-saying-.22Creation-of-element-failed..22.2C-what-should-I-do.3F)
*   [7 Plug-in Development](#Plug-in-Development)
    *   [7.1 How do I test my plug-ins?](#How-do-I-test-my-plug-ins.3F)
    *   [7.2 I get an unhandled event loop exception in my console. What gives?](#I-get-an-unhandled-event-loop-exception-in-my-console.-What-gives.3F)
    *   [7.3 A plug-in in my 'Eclipse Application' launch configuration is listed as being "out of sync", what should I do?](#A-plug-in-in-my-.27Eclipse-Application.27-launch-configuration-is-listed-as-being-.22out-of-sync.22.2C-what-should-I-do.3F)
    *   [7.4 I added a jar to my classpath, but it's not being found! What should I do?](#I-added-a-jar-to-my-classpath.2C-but-it.27s-not-being-found.21-What-should-I-do.3F)
    *   [7.5 How do I find the source for a plugin?](#How-do-I-find-the-source-for-a-plugin.3F)
    *   [7.6 How do I edit the source for a plugin?](#How-do-I-edit-the-source-for-a-plugin.3F)
*   [8 SWT](#SWT)
    *   [8.1 I cannot get the SWT widget ABC to work when I do XYZ. Could you help me?](#I-cannot-get-the-SWT-widget-ABC-to-work-when-I-do-XYZ.-Could-you-help-me.3F)
    *   [8.2 I cannot get Mozilla to run on Linux as an embedded browser?](#I-cannot-get-Mozilla-to-run-on-Linux-as-an-embedded-browser.3F)
    *   [8.3 I cannot get Firefox to run on Linux as an embedded browser?](#I-cannot-get-Firefox-to-run-on-Linux-as-an-embedded-browser.3F)
*   [9 CVS](#CVS)
    *   [9.1 I am having some trouble with CVS in eclipse -- is there a way I can get actual logs and see detailed error messages?](#I-am-having-some-trouble-with-CVS-in-eclipse----is-there-a-way-I-can-get-actual-logs-and-see-detailed-error-messages.3F)
    *   [9.2 Where are the CVS repositories locations stored?](#Where-are-the-CVS-repositories-locations-stored.3F)
*   [10 Subversion](#Subversion)
    *   [10.1 What plug-ins are there for Subversion integration?](#What-plug-ins-are-there-for-Subversion-integration.3F)
    *   [10.2 I just installed Subversion on Mac/Linux, and then I installed Subclipse, but it does not work. What is the problem?](#I-just-installed-Subversion-on-Mac.2FLinux.2C-and-then-I-installed-Subclipse.2C-but-it-does-not-work.-What-is-the-problem.3F)
    *   [10.3 I am trying to use svn+ssh:// with Subclipse, and it does not work?](#I-am-trying-to-use-svn.2Bssh:.2F.2F-with-Subclipse.2C-and-it-does-not-work.3F)
    *   [10.4 I've upgraded to Gallileo, but Subversive can't read my projects](#I.27ve-upgraded-to-Gallileo.2C-but-Subversive-can.27t-read-my-projects)
*   [11 Additions](#Additions)
    *   [11.1 Is there an UML editor for Eclipse?](#Is-there-an-UML-editor-for-Eclipse.3F)

General
-------

### I'm new, what should I read first?

*   [Recommended Eclipse Reading List](http://www.ibm.com/developerworks/opensource/library/os-ecl-read/)
*   [The Official Eclipse FAQs - Getting Started](/The_Official_Eclipse_FAQs#Getting_Started "The Official Eclipse FAQs")

### What are #eclipse and #eclipse-dev about?

*   **#eclipse** is about development _using_ Eclipse (for development _of_ Eclipse, please see **#eclipse-dev**). [log](http://echelog.com/logs/browse/eclipse)

*   **#eclipse-dev** is about development _of_ Eclipse (for development _with_/_using_ Eclipse, please see **#eclipse**). **Unless you are asking a question about how to contribute a fix to a bug or a feature enhancement, ask your Eclipse-related questions on #eclipse.** The fact of the matter is that there are a lot more people there than on #eclipse-dev.

### I am looking for help with developing with Eclipse. Should I ask on #eclipse or #eclipse-dev?

Probably **#eclipse** as long as your question relates to Eclipse usage or plug-in/RCP development. If the issue is purely a language issue unrelated to Eclipse, you should find a more appropriate channel that deals with that programming language. For example, freenode has a **##java** channel for Java-related questions. You will need to be registered to nickserv to enter that channel though.

### Are there other active channels about Eclipse?

Yes, there are [several other channels](/IRC "IRC") that are specific to projects hosted at Eclipse.org.

Others are listed below:

*   **#eclipse-e4** is the E4 discussion channel - [log](http://echelog.matzon.dk/?eclipse-e4)

*   **#eclipse-modeling** about modelling and EMF-based technologies like Xtext, Xtend, and more

*   **#azureus** about [Azureus](http://azureus.sourceforge.net), an Eclipse-based BitTorrent client

*   **#easyeclipse** about [EasyEclipse](http://www.easyeclipse.org), an Eclipse distribution

*   **#eclipselink** about [EclipseLink](https://wiki.eclipse.org/EclipseLink), persistence services project

*   **#phpeclipse** about [PHPEclipse](http://www.phpeclipse.com/), an Eclipse-based IDE for PHP

*   **#rssowl** about [RSSOwl](http://www.rssowl.org), a RSS reader built on Eclipse RCP

*   **#subclipse** about [Subclipse](http://subclipse.tigris.org), a Subversion plug-in

*   **#udig** about [UDIG](http://udig.refractions.net/confluence/display/UDIG/Home), an Eclipse-based GIS toolkit

*   **#weblogic-eclipse** about the [Webglogic](https://eclipse-plugin.projects.dev2dev.bea.com/) plug-in

*   **#higgins** about the [Higgins](http://www.eclipse.org/higgins/) Eclipse project

If you are using Linux, please consider visiting **#fedora-java**, **#gentoo-java**, and **#ubuntu-java** for distribution-specific questions.

### Nobody is answering my question. What should I do?

First, **be patient**, the person that has an answer for you may be busy at the moment. Unless you urgently need to leave, **stay**. Then please continue being patient as that person may be in another time zone. Be polite and gentle or you may be politely ignored. If nobody answers, it may just be that nobody knows the answer. You will have to do some research on your own. The [Eclipse forums](http://www.eclipse.org/forums/) are a good place, as well as [Eclipse's Bugzilla system](https://bugs.eclipse.org/bugs/). Remember that search engines are your best friends, and good luck! For those that have some idea of a possible solution, please do not hesitate to speak up!

Note also that not every project has [experts](/IRC_bot "IRC bot") or users who idle on IRC. So, here's a few other ways to get help:

1.  Read the FAQs: [Eclipse FAQs](/The_Official_Eclipse_FAQs "The Official Eclipse FAQs"), [Graphical Eclipse FAQs](/Graphical_Eclipse_FAQs "Graphical Eclipse FAQs") (these FAQs have screenshots accompanying you to guide you to the possible solution), [More FAQs](/Category:FAQ "Category:FAQ")
2.  Learn about the tools/projects you use from their [websites](http://www.eclipse.org/projects/) or [wiki pages](/Main_Page "Main Page")
3.  Read the online ISV help docs and APIs at [help.eclipse.org](http://help.eclipse.org); the search box is powerful
4.  Ask a question on the [forums](http://www.eclipse.org/forums/)
5.  Submit a question / discuss an issue / keep up to date in the [mailing lists](http://www.eclipse.org/mail/index_all.php) or search on programming sites like [StackOverflow](http://stackoverflow.com).
6.  Browse [source code in CVS](http://dev.eclipse.org/viewcvs/index.cgi) to find an existing [solution](http://dev.eclipse.org/viewcvs/index.cgi/releng-common/?root=Modeling_Project)
7.  Read [articles](http://www.eclipse.org/articles/), [blogs](http://planet.eclipse.org), and other [resources](http://www.eclipse.org/resources/)
8.  Watch [videos](http://live.eclipse.org) of demos, walkthrus, and howtos
9.  Find, report, patch, triage, fix, or verify a [bug or feature request](https://bugs.eclipse.org/bugs/)
10.  Search with [Eclipse](http://www.eclipse.org/search/search.cgi?form=extended&qprev=) or [Google](http://www.google.com/ig)
11.  Seek [legal](http://www.eclipse.org/legal/) advice about [3rd party code](http://www.eclipse.org/orbit/) and [IP](https://dev.eclipse.org/ipzilla/)

Installation, Startup and Runtime
---------------------------------

### Where can I get Eclipse?

*   Use the [downloads page](http://www.eclipse.org/downloads/) to get the latest releases. Older ones quickly become unsupported. The [comparison page](https://www.eclipse.org/downloads/compare.php?release=oxygen1a) provides a breakdown of the projects/components included in each of the pre-configured packages (eg., for Java, JEE/Web, C/C++, or RCP/Plug-in developers).
    *   You can install additional components from within Eclipse either through the Installation Manager (see **Help → Install New Software** or the **Marketplace Client**).
    *   You can use services such as [Yoxos OnDemand](http://ondemand.yoxos.com) to pick and choose Eclipse components to create a custom distribution on the fly.
*   See the archives for older [packages](http://wiki.eclipse.org/Older_Versions_Of_Eclipse) and [Eclipse Platform archives](http://archive.eclipse.org/eclipse/downloads/).
*   For the [bleeding-edge versions of the Eclipse Platform](http://download.eclipse.org/eclipse/downloads/build_types.html), visit [http://download.eclipse.org/eclipse/downloads/](http://download.eclipse.org/eclipse/downloads/) or EPP's [downloads page](http://www.eclipse.org/epp/) for bundle builds
*   Some projects, such as [BIRT](http://eclipse.org/birt), [PDT](http://download.eclipse.org/tools/pdt/downloads/), and [TPTP](http://www.eclipse.org/tptp/home/downloads/) provide "all-in-one" bundles, which include their project, Eclipse and all dependencies.

### What's the difference between all the different packages like 'Eclipse IDE for Java Developers' and the 'Eclipse IDE for Java EE Developers'? What do they contain? Do they contain source code?

See [here](https://www.eclipse.org/downloads/compare.php?release=oxygen1a).

### There is no package for the platform I'm on. What can I do?

If you are using a Oxygen release (built on Eclipse 4.7.x):

1.  Help > Install New Software...
2.  Select _Oxygen - [http://download.eclipse.org/releases/oxygen](http://download.eclipse.org/releases/oxygen)_ from the dropdown combo box.
3.  Uncheck 'Group items by category'.
4.  Then type 'epp' into the text field filter to find the package you wish to add to your existing Eclipse installation.

If you are using a Neon release (built on Eclipse 4.6.x, no longer supported):

1.  Help > Install New Software...
2.  Select _Neon - [http://download.eclipse.org/releases/neon](http://download.eclipse.org/releases/neon)_ from the dropdown combo box.
3.  Uncheck 'Group items by category'.
4.  Then type 'epp' into the text field filter to find the package you wish to add to your existing Eclipse installation.

If you are using a Mars release (built on Eclipse 4.5.x, no longer supported):

1.  Help > Install New Software...
2.  Select _Mars - [http://download.eclipse.org/releases/mars](http://download.eclipse.org/releases/mars)_ from the dropdown combo box.
3.  Uncheck 'Group items by category'.
4.  Then type 'epp' into the text field filter to find the package you wish to add to your existing Eclipse installation.

If you are using a Luna release (built on Eclipse 4.4.x, no longer supported):

1.  Help > Install New Software...
2.  Select _Luna - [http://download.eclipse.org/releases/luna](http://download.eclipse.org/releases/luna)_ from the dropdown combo box.
3.  Uncheck 'Group items by category'.
4.  Then type 'epp' into the text field filter to find the package you wish to add to your existing Eclipse installation.

If you are using a Kepler release (built on Eclipse 4.3.x, no longer supported):

1.  Help > Install New Software...
2.  Select _Kepler - [http://download.eclipse.org/releases/kepler](http://download.eclipse.org/releases/kepler)_ from the dropdown combo box.
3.  Uncheck 'Group items by category'.
4.  Then type 'epp' into the text field filter to find the package you wish to add to your existing Eclipse installation.

If you are using a Juno release (Eclipse 4.2.x, no longer supported):

1.  Help > Install New Software...
2.  Select _Juno - [http://download.eclipse.org/releases/juno](http://download.eclipse.org/releases/juno)_ from the dropdown combo box.
3.  Uncheck 'Group items by category'.
4.  Then type 'epp' into the text field filter to find the package you wish to add to your existing Eclipse installation.

If you are using an Indigo release (Eclipse 3.7.x, no longer supported):

1.  Help > Install New Software...
2.  Select _Indigo - [http://download.eclipse.org/releases/indigo](http://download.eclipse.org/releases/indigo)_ from the dropdown combo box.
3.  Uncheck 'Group items by category'.
4.  Then type 'epp' into the text field filter to find the package you wish to add to your existing Eclipse installation.

If you are using the Helios release (Eclipse 3.6.x, no longer supported):

1.  Help > Install New Software...
2.  Select _Helios - [http://download.eclipse.org/releases/helios](http://download.eclipse.org/releases/helios)_ from the dropdown combo box.
3.  Uncheck 'Group items by category'.
4.  Then type 'epp' into the text field filter to find the package you wish to add to your existing Eclipse installation.

If you are using the Galileo release (Eclipse 3.5.x, no longer supported), see [here](http://ekkescorner.wordpress.com/2009/06/30/galileo-epp-for-cocoa-64-bit/) and [here](http://bewarethepenguin.blogspot.com/2009/07/screencast-creating-eclipse-download.html) about how to "create" your own package.

[Yoxos OnDemand](http://ondemand.yoxos.com) is a free service for building a custom package.

### I don't want multiple Eclipse installations. How do I merge those packages into one?

See the solution for [There is no package for the platform I'm on. What can I do?](#There-is-no-package-for-the-platform-I.27m-on.-What-can-I-do.3F).

### How do I verify my download? Are there any MD5 or SHA1 hashes for me to verify my download against?

Eclipse.org offers MD5 and SHA1 hashes for the downloads on the main [downloads page](http://www.eclipse.org/downloads/). Click the 'More...' link for the download you are interested in and then find the 'Checksums...' link on the right hand side.

### What are all these strangely named releases?

Each year since 2006, the Eclipse community has planned, developed, and delivered a coordinated release that allows users and adopters to update their Eclipse installations at one time. These Simultaneous Releases are designated with names rather than numbers as they consist of a collection of many different projects started at different times and would have different version numbers as a result.

  

*   Oxygen is the 2017 simultaneous release of [83 major Eclipse projects](/Luna "Luna"), including release 4.7 of The Eclipse Project. For a list of those projects, what they consist of, their own individual version numbers, and an overview of some of these projects, see: [https://projects.eclipse.org/releases/oxygen1a](https://projects.eclipse.org/releases/oxygen1a)
*   Neon is the 2016 simultaneous release of [major Eclipse projects](/Luna "Luna"), including release 4.6 of The Eclipse Project. For a list of those projects, what they consist of, their own individual version numbers, and an overview of some of these projects, see: [https://projects.eclipse.org/releases/neon](https://projects.eclipse.org/releases/neon)
*   Mars is the 2015 simultaneous release of [major Eclipse projects](/Luna "Luna"), including release 4.5 of The Eclipse Project. For a list of those projects, what they consist of, their own individual version numbers, and an overview of some of these projects, see: [https://projects.eclipse.org/releases/mars](https://projects.eclipse.org/releases/mars)
*   Luna is the 2014 simultaneous release of [76 major Eclipse projects](/Luna "Luna"), including release 4.4 of The Eclipse Project. For a list of those projects, what they consist of, their own individual version numbers, and an overview of some of these projects, see: [https://projects.eclipse.org/releases/luna](https://projects.eclipse.org/releases/luna)
*   Kepler is the 2013 simultaneous release of [69 major Eclipse projects](/Kepler "Kepler"), including Eclipse 4.3. For a list of those projects, what they consist of, and an overview of some of these projects, see: [http://eclipse.org/kepler/projects.php](http://eclipse.org/kepler/projects.php)
*   Juno is the 2012 simultaneous release of [69 major Eclipse projects](/Juno "Juno"), including Eclipse 4.2. For a list of those projects, what they consist of, and an overview of some of these projects, see: [http://eclipse.org/juno/projects.php](http://eclipse.org/juno/projects.php)
*   Indigo is the 2011 simultaneous release of [62 major Eclipse projects](/Indigo "Indigo"), including Eclipse 3.7. For a list of those projects, what they consist of, and an overview of some of these projects, see: [http://eclipse.org/indigo/projects.php](http://eclipse.org/indigo/projects.php)
*   Helios is the 2010 simultaneous release of [39 major Eclipse projects](/Helios "Helios"), including Eclipse 3.6. For a list of those projects, what they consist of, and an overview of some of these projects, see: [http://eclipse.org/helios/projects.php](http://eclipse.org/helios/projects.php)
*   Galileo is the 2009 simultaneous release of [33 major Eclipse projects](/Galileo "Galileo"), including Eclipse 3.5. For a list of those projects, what they consist of, and an overview of some of these projects, see: [http://eclipse.org/galileo/projects.php](http://eclipse.org/galileo/projects.php)
*   Ganymede is the 2008 simultaneous release of [22 major Eclipse projects](/Ganymede "Ganymede"), including Eclipse 3.4. For a list of those projects, what they consist of, and an overview of some of these projects, see: [http://www-128.ibm.com/developerworks/library/os-eclipse-ganymede/](http://www-128.ibm.com/developerworks/library/os-eclipse-ganymede/)
*   Europa is the 2007 simultaneous release of [21 major Eclipse projects](/Europa "Europa"), including Eclipse 3.3. For a list of those projects, what they consist of, and how to easily get them, see: [http://www.ibm.com/developerworks/opensource/library/os-eclipse-europa/](http://www.ibm.com/developerworks/opensource/library/os-eclipse-europa/)
*   Callisto is the 2006 simultaneous release of [10 major Eclipse projects](http://www.eclipse.org/projects/callisto.php), including Eclipse 3.2.

### Where can I get a list of all the Eclipse projects?

Well, for starters, there's:

*   [Official Eclipse Projects](http://www.eclipse.org/projects/)
*   The [Eclipse Marketplace](http://marketplace.eclipse.org/)
*   [Eclipse Plug-In Central](http://www.eclipseplugincentral.com/)
*   [Eclipse Plugins](http://www.eclipse-plugins.info)
*   [Sourceforge](http://sourceforge.net/search/?type_of_search=soft&words=eclipse)

### Where can I get project XYZ?

You can search for downloads [by project](http://www.eclipse.org/downloads/index_project.php) or [by topic](http://www.eclipse.org/downloads/index_topic.php).

Or, start with that project's homepage (www.eclipse.org/xyz) and look for download or update links. For example, EMF: [website](http://www.eclipse.org/modeling/emf/), [downloads](http://www.eclipse.org/modeling/emf/downloads/), [updates](http://www.eclipse.org/modeling/emf/updates/).

Download the zip and unpack it into your Eclipse install folder, or use a .link file [\[1\]](http://divby0.blogspot.com/2007/06/managing-plugins-and-features-with-link.html) [\[2\]](http://www.ibm.com/developerworks/opensource/library/os-ecl-manage/) to locate the project in another folder.

Or, if using Update Manager, add the Update URL here, then download and install the features and plugins that way:

    Help > Install New Software > Past the URL into the Work with field
   Now select what you want to install


 

#### Is there a GUI Builder?

Yes: Google donated [WindowBuilder](http://eclipse.org/windowbuilder/) to the Eclipse Foundation. It supports round-tripping of SWT and Swing designs.

The [Visual Editor project](http://eclipse.org/vep) was decommissioned and archived in June 2011

#### Is there a PHP editor for Eclipse 3.3 Europa / Eclipse 3.4 Ganymede?

Yes, there are two. Note that installing both concurrently can be [a bad thing](/IRC_FAQ#How_do_I_manually_assign_a_project_Nature_or_BuildCommand.3F "IRC FAQ").

*   [phpeclipse](http://www.phpeclipse.de/tiki-view_articles.php) (third party)

*   Install using [Update Manager](/IRC_FAQ#Where_can_I_get_project_XYZ.3F "IRC FAQ") and the [Update Site](http://phpeclipse.sourceforge.net/update/nightly/).

*   [PHP Development Tools (PDT)](/PDT "PDT")

*   [Install like this](/PDT/Installation "PDT/Installation").

### What is p2?

[p2](/Equinox/p2 "Equinox/p2") is a provisioning system, under the Equinox project, that was introduced in Eclipse 3.4 and is intended to replace the former Update Manager.

*   [How do I update/upgrade Eclipse?](./FAQ_How_do_I_upgrade_Eclipse.md "FAQ How do I upgrade Eclipse?")

### What is the Update Manager?

**Note:** The Update Manager is now deprecated in favor of the [p2](/Equinox_p2_Update_UI_Users_Guide "Equinox p2 Update UI Users Guide") provisioning system as of the Eclipse 3.4 (Ganymede) release.

*   [p2 Users' Guide](/Equinox_p2_Update_UI_Users_Guide "Equinox p2 Update UI Users Guide")
*   [What is the Update Manager?](./FAQ_What_is_the_Update_Manager.md "FAQ What is the Update Manager?")
*   [How do I update/upgrade Eclipse?](./FAQ_How_do_I_upgrade_Eclipse.md "FAQ How do I upgrade Eclipse?")

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

*   [http://www.eclipse.org/swt/launcher.html](http://www.eclipse.org/swt/launcher.html)
*   [Eclipse runtime options](http://help.eclipse.org/indigo/topic/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html)

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

#### Shared Installation Problems

_Helios/3.6 only:_ If the installation seemed to have completed successfully but you can't seem to find anything in the user interface, you may be seeing a problem with a shared installation setup problem with Eclipse. See [bug 322929](https://bugs.eclipse.org/bugs/show_bug.cgi?id=322929) for more details. This bug will likely manifest itself if you, for example, unzipped Eclipse in the `Program Files` folder or some place like `/opt/` or `/usr/local`. The Helios SR1 (Eclipse Platform 3.6.1) release should have this problem fixed. We recommend upgrading immediately.

### How do I find out which Java runtime environment is being used to run Eclipse?

See [here](/Graphical_Eclipse_FAQs#How_do_I_find_out_which_Java_runtime_environment_is_being_used_to_run_my_Eclipse.3F "Graphical Eclipse FAQs").

### Can I upgrade from Eclipse 3.2 (Callisto) to Eclipse 3.3 (Europa)?

Yes and no.

It depends on what plugins you use and if they have been updated to work with the new version of the platform. If you used projects that were part of Callisto and which are now part of Europa, the answer is most likely yes. Some plugins will work on Eclipse 3.0 - 3.3 without any problems. Others are version-specific because they exploit aspects of Eclipse that were added or changed along the way (such as internal APIs not intended to be extended).

To upgrade, download a copy of Eclipse or a Europa bundle (see [above](/IRC_FAQ#Installation.2C_Startup_and_Runtime "IRC FAQ")). Extract the downloaded archive into its own directory (do **not** extract it into an existing eclipse directory), start up the new Eclipse and point it at your old workspace. If you see a message like 'cannot restore perspective' then something that your old workspace relied on (eg., some view contributed by some plugin) is not working anymore, and you'll have to search for an updated version from that plugin's provider.

If you installed features/plugins outside your Eclipse folder, you can reuse them via `Help > Software Updates > Manage Configuration > Add an Extension Location` to point your new Eclipse at those old plugins. However, those plugin folders must have an `.eclipseextension` file in them for Eclipse to accept them, and as noted above, they may not work without being updated. Details on using .link files and Extension Locations [here](http://divby0.blogspot.com/2007/06/managing-plugins-and-features-with-link.html) and [here](http://www.ibm.com/developerworks/opensource/library/os-ecl-manage/).

If you use JRE/JDK1.4 with Eclipse 3.2, you may need to update to JRE/JDK5.0 as some Europa components require JDK5.0 (eg., WTP, TPTP, EMF).

### Can I use my Eclipse workspace from an old release with a new Eclipse release?

Yes and no.

Your project files are compatible, but some of your settings may not be. You might want to export your settings from the old workspace before attempting to open it with the new Eclipse, then import them into the new Eclipse. For example, `Window > Preferences... > Java > Code Style > Formatter > Edit > Export`.

You might also want to start a completely new workspace and use `File > Import > Existing Projects into Workspace` to migrate the old projects into the new workspace. They can be copied or simply linked (referenced) in their old workspace location.

### How do I use a different workspace?

Three ways:

1.  In Eclipse, select `File > Switch Workspace`
2.  In Eclipse, select `Window > Preferences... > General > Startup and Shutdown > \[x\] Prompt for workspace on startup`, then restart Eclipse.
3.  Via commandline, run `./eclipse -data /path/to/new/workspace/folder`

### How do I install Eclipse on Linux?

There are many schools of thought here, depending on your comfort level with Linux and the number of users on your system.

The simplest approach is to grab the latest bundle for your distro, using something like apt-get or yum. Please note that these bundles are often not the latest and greatest because they are released in concert with the distro release cycles, not that of Eclipse. For more on these repository packages, see [Linux Distributions Project](/Linux_Distributions_Project "Linux Distributions Project"), or hang out in [#eclipse-linux on freenode](/IRC "IRC"). If you're installing on ubuntu, here's a couple walkthroughs: [Eclipse 3.4 on Ubuntu](http://jhcore.com/2008/06/26/eclipse-34-ganymede-on-ubuntu/), [Eclipse 3.2 + WTP on ubuntu, including desktop icon](http://flurdy.com/docs/eclipse/install.html).

If you want the latest, grab a [Helios](/Helios "Helios") [bundle](http://www.eclipse.org/helios) or [archive](http://download.eclipse.org/eclipse/downloads/) for your distribution. If you're on a 64-bit system and are using a 64-bit JRE, make sure you get the 64-bit version.

Save the archive into your home directory, ~ or /home/$USER/. Open a console or terminal, and type:

    cd ~; tar xvzf eclipse*.tar.gz; 
    

 

To start up Eclipse, type:

    cd ~/eclipse; ./eclipse -vm /path/to/bin/java
    

 

If you want to create a desktop or launcher shortcut, see [this ubuntu walkthrough](http://flurdy.com/docs/eclipse/install.html).

You can also install Eclipse in another location -- it's up to you, your distro's conventions, or your own preferences.

For a multi-user system, consider these [three approaches](http://divby0.blogspot.com/2007/08/howto-install-eclipse-for-multi-user.html) or [the official approach](http://help.eclipse.org/stable/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/multi_user_installs.html).

Once you have Eclipse installed, you can [add other projects to it](/IRC_FAQ#Where_can_I_get_a_list_of_all_the_Eclipse_projects.3F "IRC FAQ").

### How do I install Eclipse on Solaris?

*   For [Solaris 10 on SPARC](http://download.eclipse.org/eclipse/downloads/), get an official Eclipse build then update it using an update site such as [Ganymede](http://download.eclipse.org/releases/ganymede/).
*   For [Solaris on x86](http://code.google.com/p/solipse/), you'll have to [build it yourself](http://code.google.com/p/solipse/wiki/BasicHowToBuild) then update from an update site such as [Ganymede](http://download.eclipse.org/releases/ganymede/).

### I just unzipped Eclipse, but it does not start. Why?

[Here](./FAQ_I_unzipped_Eclipse,_but_it_won%27t_start._Why.md "FAQ I unzipped Eclipse, but it won't start. Why?") are ten possible reasons.

### I just installed Eclipse on Linux, but it does not start. What is the problem?

Quite often, Eclipse will not work properly with non-Sun or non-IBM JVMs. These issues are usually related to [Blackdown](http://www.blackdown.org/) or [GCJ](http://gcc.gnu.org/java/). Eclipse has a list of [reference platforms](http://www.eclipse.org/eclipse/development/readme_eclipse_3.4.html#TargetOperatingEnvironments) that it is tested against which you should try to match against. Try installing one from [Sun](http://java.sun.com/javase/downloads/index.jsp), [IBM](http://www.ibm.com/developerworks/java/jdk/index.html), or [BEA](http://www.bea.com/framework.jsp?CNT=index.htm&FP=/content/products/weblogic/jrockit/) (eg., [Sun JDK 5.0](http://java.sun.com/javase/downloads/index_jdk5.jsp), [IBM JDK 5.0](http://www.ibm.com/developerworks/java/jdk/linux/download.html), or [BEA JRockit 5.0](http://www.bea.com/framework.jsp?CNT=index.htm&FP=/content/products/weblogic/jrockit/)). Version 6 of [OpenJDK](http://openjdk.java.net/) works too, particularly with [Fedora Eclipse](http://fedoraproject.org/wiki/Features/Eclipse34).

Make sure that your selected JDK is the one being used when starting Eclipse by passing in the `-vm` flag.

    eclipse -vm /full/path/to/java/bin/java

It is usually desirable to place this option in your [eclipse.ini](/Eclipse.ini "Eclipse.ini") file so you don't have to type it every time you start Eclipse. If you do append it onto your eclipse.ini file, you should make sure that it appears before the `-vmargs` line, if you have one. Also note that `-vm` and the `/full/path/to/java/bin/java` are on **separate** lines.

The argument must point to the `java` or `javaw` executable, not some wrapper script like `run-java-tool`, as used by the Gentoo Linux distribution.

Please note that if you are on a 64-bit system and have a 64-bit JRE installed, you will need to use a 64-bit Eclipse build. If you wish to use a 32-bit Eclipse build, please use a 32-bit JRE. Mismatches **will** cause Eclipse to not start properly. Please see [bug 221969](https://bugs.eclipse.org/bugs/show_bug.cgi?id=221969) if you are interested in this mismatch problem.

First time downloading/installing Java for Linux? Read [this](http://java.sun.com/j2se/1.5.0/install-linux.html) or [this](http://www.ibm.com/developerworks/java/jdk/jdkfaq/).

Note also that there are differences between ext2/ext3 filesystems and fat32 ones. Eclipse includes a lot of long file paths, .dotfiles, and case sensitive files. If you plan to use Eclipse on linux, unpacking it onto an ext2 or ext3 filesystem is recommended.

### I just installed Eclipse on my 64-bit system, but it does not start. What is the problem?

Make sure that you have downloaded the 64-bit version of Eclipse (it should have x86_64 somewhere in its name) and have installed a 64-bit JVM. Likewise, if you run a 32-bit JVM, then you should use the 32-bit version of Eclipse.

### When I try to install a plug-in, I get a "Cannot connect to keystore." error, what should I do?

[Check](/Graphical_Eclipse_FAQs#How_do_I_find_out_which_Java_runtime_environment_is_being_used_to_run_my_Eclipse.3F "Graphical Eclipse FAQs") that you are not using GCJ to run Eclipse. This problem may also surface if you are using IcedTea or an OpenJDK 6 build.

Install Java 6 from Sun and use that to run Eclipse instead.

### When I start Eclipse it says "Workspace in use or cannot be created, choose a different one.", what should I do?

There are a couple of things you can try.

1.  Delete the `workspace/.metadata/.lock` file.
2.  Check your running processes to make sure there aren't any remaining Java or Eclipse processes running. When in doubt, restart your computer. :)
3.  Try starting Eclipse on a different workspace (from the workspace selection dialog, or by using a command line argument like `-data /home/user/tmp-workspace`), then [switch](#How-do-I-switch-my-workspace.3F) back to your original workspace.

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

### How come my list of update sites is completely empty when other people says theirs has stuff in it?

This is a known bug for shared installs and will likely manifest itself if you extract Eclipse in, for instance, `/opt/` on Linux or `C:\\Program Files\` on Windows. Please see [bug 249133](https://bugs.eclipse.org/bugs/show_bug.cgi?id=249133). Depending on your download and who built it, the URLs may also have been omitted.

By default, an Eclipse installation should include the following update sites:

**4.3**

*   Kepler - [http://download.eclipse.org/releases/kepler](http://download.eclipse.org/releases/kepler)
*   The Eclipse Project Updates - [http://download.eclipse.org/eclipse/updates/4.3](http://download.eclipse.org/eclipse/updates/4.3)

**3.8/4.2**

*   Juno - [http://download.eclipse.org/releases/juno](http://download.eclipse.org/releases/juno)
*   The Eclipse Project Updates - [http://download.eclipse.org/eclipse/updates/34.2](http://download.eclipse.org/eclipse/updates/34.2)

**3.7**

*   Indigo - [http://download.eclipse.org/releases/indigo](http://download.eclipse.org/releases/indigo)
*   The Eclipse Project Updates - [http://download.eclipse.org/eclipse/updates/3.7](http://download.eclipse.org/eclipse/updates/3.7)

**3.6**

*   Helios - [http://download.eclipse.org/releases/helios](http://download.eclipse.org/releases/helios)
*   The Eclipse Project Updates - [http://download.eclipse.org/eclipse/updates/3.6](http://download.eclipse.org/eclipse/updates/3.6)

### How do I install PDT?

See [here](/PDT/Installation "PDT/Installation").

### How do I install a plug-in with multiple dependencies?

There are numerous approaches here, depending on your need and starting point.

The simplest is to grab the All-In-One bundle from the project's download site, if one is provided for your platform. Note too that if you're on a 64-bit platform and are using a 64-bit JRE, you will need a 64-bit bundle (which may not exist).

But if:

*   there's no OS-compatible All-In-One available, or
*   you already have Eclipse installed, and
*   you want to add a project to an existing Eclipse, then ...

...you'll need to either:

*   download the individual required projects (most download pages have a "Requirements" section you can use for reference, but this can still be a pain), or
*   use [p2](/Equinox/p2 "Equinox/p2") to install the prereqs and the project you want, too.

Note that if you're installing from multiple update sites, you have to expand each one before you hit 'Select Required' for the wizard to be able to scan those sites and resolve all your dependencies. See [bug 203473](https://bugs.eclipse.org/bugs/show_bug.cgi?id=203473).

### How do I uninstall a plug-in?

You can view your list of installed software by checking your installation details from about dialog.

*   Help > About > Installation Details

### I'm getting "Network is unreachable" error messages when I'm trying to use the provisioning system on a Debian/Debian-based system. What should I do?

There's a setting that has been introduced regarding IPv6 that is causing this problem. Please check [Debian's bug tracking system](http://bugs.debian.org/cgi-bin/bugreport.cgi?bug=560044) for more information.

Crashers, Freezing, and other Major Issues
------------------------------------------

Do you have any `hs\_err\_pid*log` files lying around? This is an error log that is generated by HotSpot, Sun's implementation of the Java Virtual Machine (assuming that is the JVM you are using). HotSpot is also the JVM being used by the OpenJDK project, HotSpot is also being used by IcedTea (which has since been renamed to OpenJDK also).

### Eclipse is constantly crashing for me on Oracle's / Apple's Java 6 HotSpot VM...

Do your VM logs point to a problematic frame at `libjvm.so` or `jvm.dll`? Do you see any mentions of a `Current CompileTask`, possibly on `AbstractDataTreeNode`, `ParameterizedMethodBinding`, `CCPTemplates`, or `PDOMCPPLinkage`? If yes, you are likely seeing a very [infamous bug](http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6614100) in Oracle's Java 6 VM implementation. Try starting Eclipse with the most recent Java SE 6 or 7 release. Any Oracle JVM release between 1.6.0.3 and 1.6.0.9 (inclusive) are affected by this bug.

The relevant NOT_ECLIPSE bug is [bug 214092](https://bugs.eclipse.org/bugs/show_bug.cgi?id=214092) and you can read through it and its duplicates for other information. There are workarounds listed on the bug at [bug 214092 comment 13](https://bugs.eclipse.org/bugs/show_bug.cgi?id=214092#c13), [bug 214092 comment 25](https://bugs.eclipse.org/bugs/show_bug.cgi?id=214092#c25), and [bug 214092 comment 26](https://bugs.eclipse.org/bugs/show_bug.cgi?id=214092#c26). Please make sure you add the arguments after the `-vmargs` line in the `eclipse.ini` file and that the entry is on its own line.

If in doubt, you should start Eclipse with the most recent Java SE 6 or 7 release, and tell Eclipse to start use that VM to start Eclipse with the `-vm` argument (`./eclipse -vm /opt/sun-jdk-1.6.0.22/bin/java`).

### Eclipse gets past the splash screen but then an empty window appears / Eclipse is crashing on me whenever I initiate a browser component such as hovering over Java methods for javadoc tooltips...

There are a couple of possible causes to this problem.

If you have Firefox 4 installed, you should install WebKitGTK+. Alternatively, you can install an older version of XULRunner and ask Eclipse to use that XULRunner runtime. See the [SWT FAQ](http://www.eclipse.org/swt/faq.php#specifyxulrunner) for more information.

Otherwise, you want to check your workspace's log file (located in `workspace/.metadata/.log`, note the period in front of the folder and file name, this is the marker for hidden files on UNIX and UNIX-based systems) if you have one. If you do, you should search for the string `-2147467262`, the full error will be something like `org.eclipse.swt.SWTError: XPCOM error -2147467262`. If you have this problem, you should refer to [bug 213194](https://bugs.eclipse.org/bugs/show_bug.cgi?id=213194) and its other variants. This is caused by an incompatibility between the version of XULRunner detected by Eclipse and Eclipse itself. It is highly recommend that you uninstall any alpha or beta versions of XULRunner on your system since those are not supported. Alternatively, you can install a recent stable version of Firefox/XULRunner and point Eclipse at that particular runtime. See the [SWT FAQ](http://www.eclipse.org/swt/faq.php#specifyxulrunner) for more information. If possible, you should upgrade to Eclipse 4.2.x as soon as possible since the older releases are no longer under maintenance.

If you are seeing the problem with the empty window, that too should be resolved by what has been mentioned above. Please refer to [bug 234910](https://bugs.eclipse.org/bugs/show_bug.cgi?id=234910) for more information.

If you did not find the XPCOM error, did you find any references to `libxul.so` or `xul.dll` in those logs? Maybe you see a mention of `NewLocalFile` in there too? If yes, you have likely hit [bug 236724](https://bugs.eclipse.org/bugs/show_bug.cgi?id=236724). The cause of this problem is explained in [bug 236724 comment 44](https://bugs.eclipse.org/bugs/show_bug.cgi?id=236724#c44). To workaround this problem in the 3.3.x line, you should take a look at [bug 236724 comment 22](https://bugs.eclipse.org/bugs/show_bug.cgi?id=236724#c22). If you are comfortable using a newer build, you should try the most recent release or maintenance build. These builds can be found [here](http://download.eclipse.org/eclipse/downloads/).

### The Java EE build for Eclipse Ganymede / Eclipse 3.4 does not run for me but the Eclipse Classic build starts up just fine...

Do you see something like `-vm C:\\Program Files\\Java\\jre1.6.0_07\\bin\\server\\jvm.dll` in the error dialog? If you are using a `dll` or `so` as an argument to `-vm` for starting up Eclipse, there may be problems, see [bug 237652](https://bugs.eclipse.org/bugs/show_bug.cgi?id=237652) for more information. You should be able to work around this problem by using the actual `java` binary as an argument to `-vm`.

### Why does Eclipse hang for an extended period of time after opening an editor in Linux/gtk+?

There seems to be a bug in gtk+ 2.10 and 2.12 that is causing certain printing backends to hang. You can try to start Eclipse with the method suggested below. This should be supported in 3.3.1 (and up) maintenance builds and 3.4 and upwards.

    ./eclipse -vmargs -Dorg.eclipse.swt.internal.gtk.disablePrinting
    

 

You should consider taking a look at [bug 215234](https://bugs.eclipse.org/bugs/show_bug.cgi?id=215234) for more information. You may also want to refer to [bug 346903](http://bugzilla.gnome.org/show_bug.cgi?id=346903) in GNOME's bugzilla system.

### Eclipse is crashing for me when performing Subversion operations on Windows. I am using Subclipse.

Have you found any JVM error logs with a mentioning of the libapr-1.dll file? If yes, you have hit a DLL incompatibility problem raised by Subversion's use of the Apache Portable Runtime library. Please see Subclipse's [FAQ entry](http://subclipse.tigris.org/wiki/PluginFAQ#head-43aec0a3a0f7175fc55e72caf95471f8711d9d28) about this for more information.

### I'm having memory, heap, or permgen problems, what can I do?

*   [FAQ How do I increase the heap size available to Eclipse?](./FAQ_How_do_I_increase_the_heap_size_available_to_Eclipse.md "FAQ How do I increase the heap size available to Eclipse?")
*   [FAQ How do I increase the permgen size available to Eclipse?](./FAQ_How_do_I_increase_the_permgen_size_available_to_Eclipse.md "FAQ How do I increase the permgen size available to Eclipse?")

### Eclipse is constantly crashing for me on Linux and I have a lot of plug-ins installed. There is a mentioning of "too many open files".

With around 1000 plug-ins installed on Linux, Eclipse will crash due to an excessive amount of open files. Please see [bug 238015](https://bugs.eclipse.org/bugs/show_bug.cgi?id=238015) for more information. The best workaround at the moment is to add `osgi.bundlefile.limit=600` in `eclipe/configuration/config.ini`.

### Eclipse buttons in dialogs and other places are not working for me if I click them with the mouse. I also cannot see anything in the tree when I try to install updates. What's going on?

Are you using running on GTK+ 2.17.x/2.18.x? Try declaring the GDK\_NATIVE\_WINDOWS environment variable and see how that goes. (Note that for some folk this workaround causes other failures.)

    $ export GDK_NATIVE_WINDOWS=true
    $ ./eclipse
    

 

The latest version of GTK+ has altered the way it creates and handles windows on the X windowing system. This has introduced some problems due to the way SWT was managing the Z order of windows.

The buttons problem should be fixed in both the 3.6 stream and the 3.5.2 stream. Any [3.5.2 stream build](http://download.eclipse.org/eclipse/downloads/) newer than M20091118-0800 should include the fix. See [bug 291257](https://bugs.eclipse.org/bugs/show_bug.cgi?id=291257) for more information.

The tree problem is described by [bug 290395](https://bugs.eclipse.org/bugs/show_bug.cgi?id=290395). This has been fixed in the 3.6 stream. Any build newer than I20091201-2000 should include the fix. Please test it out to see if it resolves your problem. Pending community feedback, the fix may be backported to 3.5.2

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

### How to have a custom .php extension recognized by PDT (or .xml by the XML editor and other XML features, etc.)?

Some people like to change the standard .php extension of their php files to something else (e.g. ArsTechnica does it with their .ars).

If you do this, though, PDT probably won't recognize your php files (and many features including Syntax highlighting, Debugging, etc will be lost). In order to instruct Eclipse to treat your .xyz files as php content, you must go to:

    Windows > Preferences > General > Content Types > Text > PHP Content Type
    

 

and add *.xyz

Note: you'll also have to instruct your http server that .xyz are php content. With an Apache server, one way to do this is to add a configuration directive like "AddType application/x-httpd-php .xyz".

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

Check the [SWT Snippets](http://www.eclipse.org/swt/snippets/) section, there might be a code example that demonstrates what you are trying to do.here.

### I cannot get Mozilla to run on Linux as an embedded browser?

Follow the instructions in the [SWT FAQ](http://www.eclipse.org/swt/faq.php#browserlinux).

### I cannot get Firefox to run on Linux as an embedded browser?

Firefox is not supported as embedded browser on Linux for now. Take a look at [bug #72487](https://bugs.eclipse.org/bugs/show_bug.cgi?id=72487).

On some Linux distributions that dynamically link Firefox (like Ubuntu and Fedora), Firefox will work.

CVS
---

### I am having some trouble with CVS in eclipse -- is there a way I can get actual logs and see detailed error messages?

Open the console view and select CVS from the triangle drop-down in the console toolbar.

### Where are the CVS repositories locations stored?

In <workspace>/.metadata/.plugin/org.eclipse.team.cvs.ui/repositoriesView.xml . Previously used commit comments are in commitCommentHistory.xml.

Subversion
----------

### What plug-ins are there for Subversion integration?

You can try either [Subclipse](http://subclipse.tigris.org/) or [Subversive](http://www.eclipse.org/subversive). Don't ask us which one is better. Some people use Subclipse, some people use Subversive.

### I just installed Subversion on Mac/Linux, and then I installed Subclipse, but it does not work. What is the problem?

Verify that the version of Subclipse matches the version of Subversion that you have installed. Try uninstalling Subversion and configuring Subclipse's preferences to run JavaSVN. Take a look at the [Subclipse FAQ](http://subclipse.tigris.org/faq.html) and the [JavaHL FAQ](http://subclipse.tigris.org/wiki/JavaHL). Ask on **#subclipse** for additional assistance.

### I am trying to use svn+ssh:// with Subclipse, and it does not work?

Generally speaking, the JavaSVN adapter in the Team/SVN preferences will yield better results when using svn+ssh. See: [Subclipse support for SVN protocols](http://svn.collab.net/subclipse/help/index.jsp?topic=/org.tigris.subversion.subclipse.doc/html/reference/protocol.html) for detailed information.

### I've upgraded to Gallileo, but Subversive can't read my projects

It seems the versions of Subversive for Ganymede and Gallileo use different versions of the SVNKit plugin. Backing out to a previous version of SVNKit 1.3.2 may solve the issue.

Additions
---------

### Is there an UML editor for Eclipse?

*   An Eclipse Modelling project-based UML editor can be installed from the Eclipse update site "Modelling > UML2 Tools SDK". See [Creating UML 2 diagrams with Eclipse UML2 Tools - Tutorial](http://www.vogella.de/articles/UML/article.html) for an introduction.

