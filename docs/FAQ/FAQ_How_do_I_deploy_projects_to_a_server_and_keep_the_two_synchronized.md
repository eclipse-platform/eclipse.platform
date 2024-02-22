

FAQ How do I deploy projects to a server and keep the two synchronized?
=======================================================================

  

  

Some development tools include support for pushing projects to a remote server, using such protocols as FTP and WebDAV. Then an ongoing challenge is to keep the old workspace contents synchronized with the remote content as files in the workspace and on the remote server are added, removed, or changed. The Eclipse team API includes generic infrastructure for deploying workspace contents to a remote location and for keeping the two copies synchronized.

  
The main entry point for this kind of team integration is the notion of a _team subscriber_. A subclass of TeamSubscriber specifies the logic for comparing workspace contents to a remote resource and for performing synchronization. The team API has support for building and maintaining a model of remote resources and the synchronization state between remote and local resources. A subscriber can use the generic Synchronize view to allow users to browse the differences between local and remote copies and for refreshing and synchronizing the two. The org.eclipse.team.ui.synchronize package includes API for adding pages to the Synchronize view and for displaying the synchronization model created by a team subscriber.

  
See the _Platform Plug-in Developer Guide_ under **Programmer's Guide > Team support** for complete details on how to implement your own team subscriber and for integrating with the generic Synchronize view.


