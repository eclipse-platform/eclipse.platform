FAQ How do I store extra properties on a resource?
==================================================

Several mechanisms are available for attaching metadata to resources. The simplest are session and persistent properties, which affix arbitrary objects (for session properties) or strings (for persistent properties) to a resource keyed by a unique identifier. Session properties are discarded when the platform shuts down, and persistent properties are saved on disk and will be available the next time the platform starts up. Markers are intended to work as annotations to a particular location in a file but in reality can be used to attach arbitrary strings, integers, or Boolean values to a resource. Synchronization information can also be attached to a resource (see ISynchronizer). Sync info was designed for clients, such as repositories or deployment tools, that need to track differences between local resources and a remote copy. Such clients have some very special features, including the ability to maintain the metadata regardless of whether the resource exists. Although quite specialized, this form of metadata may be appropriate for use in your plug-in. Finally, there is the Eclipse preference API, which has a special node for storing project-specific metadata (see ProjectScope). A unique attribute of preferences is that they are stored inside the project location, so that the data is shared when the project is exported or checked into a repository.

Table 17.1 provides a high-level view of the various forms of metadata, along with some of their key design features. _Speed_ refers to the amount of CPU time required for typical access and storage operations. _Footprint_ refers to the memory space required for storing the information during a session. Persistent properties are not stored in memory at all, except for a small cache, which makes them good memory citizens but terrible for performance. _Notify_ refers to whether resource change notification includes broadcasting changes to this metadata. Note that this is not always a good thing: Performing a resource change broadcast has a definite performance cost, which will be incurred whether or not you care about the notification. Also, metadata that is included in resource change events cannot be changed by a resource change listener. So if you need a resource change listener to store some state information, you're stuck with either session or persistent properties. Note that preferences are not included in resource change events, but rather have their own separate notification mechanism. _Persistence_ describes when the information is written to disk. Full save happens when the platform is being shut down, and snapshot is a quick incremental save that happens every few minutes while the platform is running. _Data types_ identify the Java data types that can be stored with that metadata facility.

|   | **Speed** | **Footprint** | **Notify** | **Persistence** | **Types** | **Size constraints?** |
| --- | --- | --- | --- | --- | --- | --- |
| Markers | Good | Medium | Yes | Save; snapshot | String, int, boolean | No |
| Sync info | Good | Medium | Yes | Save; snapshot | byte[] | ?? |
| Session Property | Fast | Medium | No | None | Object | No |
| Persistent Property | Slow | None | No | Immediate | String | Yes: 2K, exception thrown on overflow |
| Project Preferences | Slow | Small (preference cache) | Yes | Immediate | String, all primitive types | No |

**Table 17.1**   Forms of resource metadata

  

Keep in mind that the best solution for your particular situation isn't always as simple as picking one of these four mechanisms. Sometimes a combination of these systems works better. For example, session properties can be used as a high- performance cache during an operation, and the information can then be flushed to a persistent format, such as persistent properties, when your plug-in is not in use. Finally, for large amounts of metadata, it is often better to store the information in a separate file. You can ask the platform to allocate a metadata area for your plug-in, using IProject.getWorkingLocation, or you can store metadata directly in the project's content area so that it gets shared with the user's repository.

See Also:
---------

*   [FAQ How do I create my own tasks, problems, bookmarks, and so on?](./FAQ_How_do_I_create_my_own_tasks_problems_bookmarks_and_so_on.md "FAQ How do I create my own tasks, problems, bookmarks, and so on?")
*   [FAQ How can I be notified on property changes on a resource?](./FAQ_How_can_I_be_notified_on_property_changes_on_a_resource.md "FAQ How can I be notified on property changes on a resource?")

