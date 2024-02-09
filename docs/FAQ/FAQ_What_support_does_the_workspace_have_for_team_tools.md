FAQ What support does the workspace have for team tools?
========================================================

  
  

Repository tools often need special control over how files in the workspace are managed. The workspace API includes special hooks that allow repository tools to reimplement certain methods, validate and veto file changes, and store synchronization state on resources. These facilities are generally well described in the _Platform Plug-in Developer's Guide_, but here is a quick tour of special repository integration support in the workspace:

*   IFileModificationValidator. This hook is called when a

file is about to be edited and immediately prior to a file being saved. This gives repository providers a chance to check out the file being changed or to veto the modification. A repository provider can supply a validator by overriding getFileModificationValidator on RepositoryProvider.</li>

*   IMoveDeleteHook. This hook allows repository providers

to reimplement the resource move and delete methods. This is used both for validation prior to move and deletion and for transferring version history when a resource is moved. This hook is supplied by overriding getMoveDeleteHook on RepositoryProvider.</li>

*   TeamHook. The workspace API developers realized that the

existing hook methods could not be extended without breaking API compatibility. To avoid an explosion of specialized hook methods, the developers decided to consolidate future team-integration hooks in a single place. This hook is currently used only for validating linked resource creation, but any future hooks that are added for team plug-ins will appear here. </li>

*   ISynchronizer. This API associates synchronization information,

such as file timestamps, with a resource. The synchronization information is represented as arbitrary bytes, so you can store whatever information you want, using the synchronizer. The workspace will take care of storing and persisting this information across sessions and will broadcast resource change notifications when this information changes. </li>

  

Note that although these facilities were designed for use by repository tools, sneaky plug-ins have been known to use them for other purposes. If you have very specialized needs in your plug-in, it may be justifiable to use some of these hooks yourself. For example, the plug-in development tools use these hooks for binary plug-in projects to prevent users from deleting content that is linked back into your install directory. Keep in mind that in the case of the hook methods, a project can have only one hook implementation. Using these hooks for your plug-in will prevent other repositories from being able to connect to those projects.

  

See Also:
---------

[FAQ\_What\_APIs\_exist\_for\_integrating\_repository\_clients\_into_Eclipse?](./FAQ_What_APIs_exist_for_integrating_repository_clients_into_Eclipse.md "FAQ What APIs exist for integrating repository clients into Eclipse?")

