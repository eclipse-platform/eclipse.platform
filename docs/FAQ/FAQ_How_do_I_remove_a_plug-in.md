

FAQ How do I remove a plug-in?
==============================

Depending upon the particular version of Eclipse you are running with, the difficulty of uninstalling features or plugins from an Eclipse installation ranges from trivial to painful. In some cases, Eclipse doesn't support uninstalling certain 'optional' features after they have been installed.

Run **Help > About Eclipse > Installation Details**, select the software you no longer want and click **Uninstall**. (On Macintosh it is **Eclipse > About Eclipse > Installation Details**.)

In older versions, you might need to Run **Help > Software Updates > Manage Configuration...**, select the feature of interest, and disable it with the task shown in the right window.

'Uninstalling' a feature, using the steps above, disable the feature or plug-in. These steps are using the Update Manager under the covers.

When a feature is disabled, all its plug-ins will be disabled also. They are still available on disk, and they can be enabled at any time in the future.

There is no mechanism within Eclipse to permanently and physically uninstall a feature and its plug-ins. The process to physically and permanently remove an undesirable feature and its plug-ins is a manual process that should be done when Eclipse is not running. In order to do, you will have to manually remove the files there associated with the feature from the eclipse/features directory and its plug-ins from the eclipse/plugins directory. Be very cautious as to which files you delete, and always have a backup of your Eclipse directory. If you remove the wrong files from these directories, you may have quite some trouble restoring your Eclipse to a stable state. Therefore, unless your hard disk storage capacity is extraordinarily limited, it is recommended that you simply leave the physical files in place.

Note that when manually removing plugins as described above, it is likely that some metadata will still cached by Eclipse. This can lead to problems later on. Running Eclipse with the -clean option may help with that, as it causes Eclipse to clean the cached metadata. See the [Running Eclipse](https://help.eclipse.org/topic/org.eclipse.platform.doc.user/tasks/running_eclipse.htm?cp=0_3_0) help page for details about this option.

Comments from the peanut gallery
--------------------------------

There are legitimate use cases for wanting to safely remove a particular plugin, such as rolling back to a previous version, repairing a corrupted plugin, or eliminating conflicting plugins?

Under Helios/3.6/STS it's impossible to remove plug-ins or disable features. You can disable a few plugins at startup via "Window > Preferences" from the menu under "General > Startup and Shutdown". Uncheck the items you don't want to run when you start Eclipse. This needs to be done for every workspace. To remove a plug-in you need to remove the JAR file from the "plugins" directory, located in your Eclipse installation directory.

Under Indigo/3.7 the "Help > About Eclipse > Installation Details > Installed Software tab > Uninstall..." option works. For some.

Under Helios I had none of the menu items mentioned above. I went to Help -> Install New Software... ; clicked the small blue hyperlink for What is "already installed"; and under the Installed Software tab, this gave me (contrary to the notes above) the option to uninstall things. I used it to uninstall PyDev, which was creating conflicts for me with Java development.

This is the type of error message gives rise to the typical use-case for uninstalling a plugin: `Cannot complete the install because of a conflicting dependency.`

 

      Software being installed: m2e - Maven Integration for Eclipse 1.3.1.20130219-1424 (org.eclipse.m2e.feature.feature.group 1.3.1.20130219-1424)
      Software currently installed: Project configurators for commonly used maven plugins (temporary) 0.12.0.20101103-1500 (org.maven.ide.eclipse.temporary.mojos.feature.feature.group 0.12.0.20101103-1500)
      Only one of the following can be installed at once: 
        Maven Integration for Eclipse 0.12.1.20110112-1712 (org.maven.ide.eclipse 0.12.1.20110112-1712)
        This version of m2eclipse cannot be installed on top of the already installed m2eclipse. Uninstall the previous version of m2eclipse and try the install again. 1.3.1.20130219-1424 (org.maven.ide.eclipse 1.3.1.20130219-1424)
      Cannot satisfy dependency:
        From: m2e - Maven Integration for Eclipse 1.3.1.20130219-1424 (org.eclipse.m2e.feature.feature.group 1.3.1.20130219-1424)
        To: bundle org.maven.ide.eclipse [1.3.1.20130219-1424]
      Cannot satisfy dependency:
        From: Plexus Metadata Generation 0.12.0.20101103-1500 (org.maven.ide.eclipse.plexus.annotations 0.12.0.20101103-1500)
        To: bundle org.maven.ide.eclipse [0.10.0,0.13.0)
      Cannot satisfy dependency:
        From: Project configurators for commonly used maven plugins (temporary) 0.12.0.20101103-1500 (org.maven.ide.eclipse.temporary.mojos.feature.feature.group 0.12.0.20101103-1500)
        To: org.maven.ide.eclipse.plexus.annotations [0.12.0.20101103-1500]
    

 

