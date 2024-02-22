FAQ How do I upgrade Eclipse IDE?
=================================

(Redirected from [FAQ How do I upgrade Eclipse?](/index.php?title=FAQ_How_do_I_upgrade_Eclipse%3F&redirect=no "FAQ How do I upgrade Eclipse?"))

Contents
--------

*   [1 Upgrading existing Eclipse IDE and Installed Features to newer release](#Upgrading-existing-Eclipse-IDE-and-Installed-Features-to-newer-release)
*   [2 Always enable major upgrades](#Always-enable-major-upgrades)
*   [3 Beta-testing milestones and release candidates](#Beta-testing-milestones-and-release-candidates)
*   [4 Fresh install](#Fresh-install)
*   [5 Windows specifics](#Windows-specifics)
*   [6 Older versions (deprecated)](#Older-versions-.28deprecated.29)
    *   [6.1 Upgrading from previous versions to Neon (4.6) is NOT supported](#Upgrading-from-previous-versions-to-Neon-.284.6.29-is-NOT-supported)
    *   [6.2 Upgrading from Ganymede (3.4)](#Upgrading-from-Ganymede-.283.4.29)
    *   [6.3 Upgrading from Europa (3.3) and below](#Upgrading-from-Europa-.283.3.29-and-below)
    *   [6.4 Upgrading Other Features](#Upgrading-Other-Features)
*   [7 See Also:](#See-Also:)

Upgrading existing Eclipse IDE and Installed Features to newer release
----------------------------------------------------------------------

To upgrade Eclipse IDE to the next major release

1.  You first need to [add the new release's repository](https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.user%2Ftasks%2Ftasks-128.htm) as follows:
    1.  Window > Preferences > Install/Update > Available Software Sites
    2.  Click 'Add'
    3.  Enter the URL of the new repository for example, https://download.eclipse.org/releases/2021-12/
    4.  Click 'Ok'
2.  [Help > Check for Updates](https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.user%2Ftasks%2Ftasks-120.htm)
3.  If updates are found, proceed through the install wizard and restart the IDE when prompted. Otherwise, read carefully the error message to find out which component is conflicting and establish your resolution strategy.
4.  Note that the start splash screen may be cached and will not necessarily be updated to the latest version after the IDE is restarted. Performing a full relaunch should display the new version number.

Always enable major upgrades
----------------------------

To always enable major upgrades of your IDE once and for all:

1.  Open the _Available Software Sites_ preference page
2.  Enable the [Latest Eclipse release](https://download.eclipse.org/releases/latest) repository by ticking the checkbox.
3.  Apply and Close
4.  Check for updates

The similar workflow can be used to hide and disable automatic proposal of major upgrades.

Beta-testing milestones and release candidates
----------------------------------------------

The same process as above can be used to enable update to milestones or release candidates of the Eclipse IDE (which have already been partially tested before being published, but might still contain unknown issues for you to [report to Bugzilla](https://bugs.eclipse.org/bugs/enter_bug.cgi)). The only difference is that you should add the 2 following URLs as [Available sites](https://help.eclipse.org/2018-12/index.jsp?topic=%2Forg.eclipse.platform.doc.user%2Ftasks%2Ftasks-128.htm) before running [Check for updates](https://help.eclipse.org/2018-12/index.jsp?topic=%2Forg.eclipse.platform.doc.user%2Ftasks%2Ftasks-120.htm) in order to let Eclipse IDE locate the milestones/release-candidates:

*   Assuming next release name is _2021-12_
    *   https://download.eclipse.org/staging/2021-12/
    *   https://download.eclipse.org/releases/2021-12/ (this is the same URL that will be used for release)

Fresh install
-------------

If you prefer not performing an update (for example because some 3rd-party content is not ready for the current release of Eclipse IDE and the update reports conflicts), you can still download a fresh install of the Eclipse IDE and install it in another location on your filesystem, and use it together with the previous version.

To do so, download a new build from the [Eclipse download website](https://www.eclipse.org/downloads/eclipse-packages/) and run the installer or unzip the archive in a new directory. We strongly recommend against installing/unzipping over your existing version of Eclipse IDE as it may corrupt your installation.

When you start a new version of Eclipse IDE, you can use the same existing workspace folder that you were using with the older version. The workspace will be migrated to a newer version and the Eclipse IDE will reuse all configurations. The workspace is forward compatible, but might not be backward compatible.

Windows specifics
-----------------

If Eclipse IDE is installed in a restricted directory, upgrades may require administrator privileges to succeed and may fail with error messages claiming "Only one of the following can be installed:" otherwise. Start Eclipse with "Run as administrator...".

Older versions (deprecated)
---------------------------

### Upgrading from previous versions to Neon (4.6) is NOT supported

**Upgrading from Mars (4.5) and older Eclipse IDE package to Neon (4.6) is NOT supported** _NOTE: Due to structural changes you cannot update from a Mars (or prior) all-in-one package to a Neon version. If interested in the technical details, see [bug 332989](https://bugs.eclipse.org/bugs/show_bug.cgi?id=332989) and [bug 490515](https://bugs.eclipse.org/bugs/show_bug.cgi?id=490515). So to use Eclipse Neon IDE, you have to go for a [#Fresh Install](#Fresh-Install). From Neon to Oxygen and later, the usual upgrade process detailed above works._

### Upgrading from Ganymede (3.4)

If you're using **Ganymede (3.4)**: To upgrade installed software, do the following:

1.  Help > Software Updates...
2.  Switch to the 'Installed Software' pane
3.  Select one or more installed items to be upgraded. If nothing is selected, it will search for updates to all installed software
4.  Select 'Update', and proceed through the wizard if updates are found

### Upgrading from Europa (3.3) and below

If you're using **Europa (3.3) and below**: Run the Update Manager, using Help > Software Updates > Find and Install... > Search for updates of the currently installed features. The Update Manager will visit the Update site(s) for all your installed features/plugins and offer updates if any exist. However, in Eclipse 3.3 or earlier, it is _**NOT**_ possible to upgrade the Eclipse platform itself, only its features. So, you could for example upgrade the CVS feature or the PDE feature from 3.2.0 to 3.2.1, but not eclipse.exe itself.

### Upgrading Other Features

Upgrading other features (like CDT, PDT, WTP...) can be done without the need to [download a new platform binary](https://download.eclipse.org/eclipse/downloads/), but because many projects align very closely (eg., the [Eclipse 3.2 / Callisto](https://www.eclipse.org/callisto/) or [Eclipse 3.3 / Europa](https://www.eclipse.org/europa/) release trains) you will likely need to upgrade the Eclipse platform as well.

See Also:
---------

*   [FAQ What is the Update Manager?](./FAQ_What_is_the_Update_Manager.md "FAQ What is the Update Manager?") 
*   [How to upgrade Eclipse for Java EE Developers from Juno to Kepler?](https://stackoverflow.com/questions/17337526/how-to-upgrade-eclipse-for-java-ee-developers-from-juno-to-kepler)

  

