Semantic File System GettingStarted
================================================

Contents
--------

*   [1 Prerequisites](#Prerequisites)
*   [2 Creating a Semantic project](#Creating-a-Semantic-project)
*   [3 A short walk-through: some places to know about when working with the Semantic File System](#A-short-walk-through:-some-places-to-know-about-when-working-with-the-Semantic-File-System)
    *   [3.1 Semantic Properties Page](#Semantic-Properties-Page)
    *   [3.2 Team Provider menu](#Team-Provider-menu)
    *   [3.3 Semantic File System Preference Page](#Semantic-File-System-Preference-Page)
    *   [3.4 Semantic Content View](#Semantic-Content-View)
    *   [3.5 Team Synchronizer UI](#Team-Synchronizer-UI)
    *   [3.6 Remote History Page](#Remote-History-Page)
*   [4 The DefaultContentProvider](#The-DefaultContentProvider)
    *   [4.1 Local File handling](#Local-File-handling)
    *   [4.2 REST resource handlingâInternet URL](#REST-resource-handling.E2.80.94Internet-URL)
*   [5 Using Semantic Resources outside of Semantic Projects](#Using-Semantic-Resources-outside-of-Semantic-Projects)

Prerequisites
-------------

You have an Eclipse IDE with the Semantic File System plug-ins running.  
The required plug-ins are:

*   org.eclipse.core.resources.semantic
*   org.eclipse.ui.resources.semantic

The IDE used for the below examples was a Helios âEclipse IDE for RCP and RAP Developersâ downloaded from [https://www.eclipse.org/downloads](https://www.eclipse.org/downloads). Other Eclipse IDE 3.5 and 3.6 versions using a 5.0 or 6.0 Java runtime should also work.

Creating a Semantic project
---------------------------

Before you can start, you need to create a project located in the Semantic File System (also referred to as "Semantic Project"). Since the "Generic Project" wizard offers to do this in the UI, it can be used to do so. The wizard can be started using New -> Project..., then General -> Project:

 ![NewSemanticProjectWizard.jpg](https://raw.githubusercontent.com/https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/docs/master/docs/images/NewSemanticProjectWizard.jpg)

We use "SemanticProject" both for the project name and the location. Note that you have to unselect "Default Location" and select "Semantic File System" in the "Choose file system" dropdown. After the project has been created, it must be shared with the Semantic File System Team provider. Select Team -> Share Project..., then "Semantic File System" and "Finish".

 ![SFSShareProjectWizard.jpg](https://raw.githubusercontent.com/https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/docs/master/docs/images/SFSShareProjectWizard.jpg)

A short walk-through: some places to know about when working with the Semantic File System
------------------------------------------------------------------------------------------

#### Semantic Properties Page

This can be reached from any Semantic Resource by opening the context menu in the project explorer and selecting Properties. On the left hand side, there should be an entry âSemantic Resourceâ. If you select this, you can inspect some resource properties specific to Semantic Resources, for example for the new Semantic Project:

![SFSDemoSFSProperties.jpg](https://raw.githubusercontent.com/https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/docs/master/docs/images/SFSDemoSFSProperties.jpg)

By right-clicking on any entry, you can copy either the value or the key and the value into the clipboard. If you have selected more than one entry, you can copy all selected entries into the clipboard (keys and values).

#### Team Provider menu

This can also be reached from any Semantic Resource by opening the context menu and then sub-menu âTeamâ. The available menu entries depend on the selected resource:

![SFSDemosTeamMenu.jpg](https://raw.githubusercontent.com/https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/docs/master/docs/images/SFSDemosTeamMenu.jpg)  

#### Semantic File System Preference Page

This shows the location of the database file and the cache root which may be useful if you want to have a look at the actual data in your workspace.

#### Semantic Content View

This is a low-level view of the data in the Semantic File System database:  
![SFSDemosSemanticResourcesView.jpg](https://raw.githubusercontent.com/https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/docs/master/docs/images/SFSDemosSemanticResourcesView.jpg)  
It can be reached from any Semantic Resource using Team->Show in Semantic Content View.

The view is refreshed automatically to reflect chnages in the Semantic File System; auto-refresh can be disabled in the view menu.

By right-clicking on a node, you can perform the following actions:  

*   Delete the node and it's children (if any)
*   Forcefully delete the node and it's children
*   Open the node in the Properties View
*   Copy the node's path into the clipboard
*   Open the node in a text editor (file nodes only)

The second option can be helpful to cleanup a corrupt SFS database.

When a node is opened in the Properties View, the same information is shown as described above in the section "Semantic Properties Page".

#### Team Synchronizer UI

This is currently not fully supported. Please see the description of the RemoteStoreContentProvider in [http://wiki.eclipse.org/E4/Resources/Semantic\_File\_System/Demos/Running\_SFS\_Examples](http://wiki.eclipse.org/E4/Resources/Semantic_File_System/Demos/Running_SFS_Examples) for more details.  

#### Remote History Page

The generic âHistoryâ view is found under category âTeamâ. Depending on whether a certain content provider implements history support, it will show the remote versions for a Semantic File.

The DefaultContentProvider
--------------------------

The DefaultContentProvider is used for any resources for which no specific content provider is responsible. This is necessary, since the project itself is a Semantic Resource. In other words, the DefaultContentProvider bridges the Semantic File System world and the Local File System world.  
In addition, the DefaultContentProvider allows some basic REST-like Resource management.

#### Local File handling

Local files are handled by the DefaultContentProvider in a transparent manner. You can create a local file in the Semantic Project by simply selecting the context menu of the project and clicking New -> File. A dialog will be shown asking for a file name. Use "localFile.txt" and hit "Finish". The new file will be shown in an editor. You can edit the file content just like with any file. A blue decorator indicates that the file is local. Still the file is a Semantic Resource (try it and check the Semantic Properties page mentioned above):

![SFSDemosLocalResourceProperties.jpg](https://raw.githubusercontent.com/https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/docs/master/docs/images/SFSDemosLocalResourceProperties.jpg)

#### REST resource handlingâInternet URL

Select the project and open the context menu. Select New... -> Other, then Semantic File System -> REST Resource. A dialog will show up requesting a name and a URL for the new resource. Enter some arbitrary name, and an existing (and accessible) URL, for example âtime.htmlâ and â[http://www.time.orgâ](http://www.time.orgâ):  
![SFSDemosNewURLResource.jpg](https://raw.githubusercontent.com/https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/docs/master/docs/images/SFSDemosNewURLResource.jpg)  

Open the new file with the text editor. You will see some HTML code containing a line like  
<title>Welcome To The WWW Clock (beta) - Wednesday 16th of December 2009 9:25:39 AM GMT</title>  
If you close and re-open the editor, this line will not change. However, you can retrieve the new content using "Team" -> "Synchronize with Repository" on the context menu of the new file:  
![SFSDemosSyncUrlFromRemote.jpg](https://raw.githubusercontent.com/https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/docs/master/docs/images/SFSDemosSyncUrlFromRemote.jpg)  
Afterwards, the line above should have changed.  
The default content provider does not support editing of non-local resources, so you cannot change the content of the file.

Using Semantic Resources outside of Semantic Projects
-----------------------------------------------------

It is possible to create Semantic Resources outside of Semantic Projects. Technically, this is done by creating a link to a resource in the Semantic File System. A generic UI is provided to add a REST resource using the DefaultContentProvider. You can open this UI using File->New->Other... (or CTRL-N) and then Semantic File System->REST Resource. The action can also be called from the context menu of any resource. You will see a screen asking for a parent, a name, and a URL. Depending on the context, the parent may be pre-set. Let's assume we have a "General" project called "GeneralProject" and a folder f1 from which we create the resource:

 ![NewRestResource.jpg](https://raw.githubusercontent.com/https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/docs/master/docs/images/NewRestResource.jpg)

After successful completion of the wizard, the new resource will be shown as linked file under the folder and can be used just like any linked file:

 ![NewLinkedResource.jpg](https://raw.githubusercontent.com/https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/docs/master/docs/images/NewLinkedResource.jpg)

It will also appear in the Semantic Content View under the same path as the linked resource:

 ![NewLinkedInContentView.jpg](https://raw.githubusercontent.com/https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/docs/master/docs/images/NewLinkedInContentView.jpg)

