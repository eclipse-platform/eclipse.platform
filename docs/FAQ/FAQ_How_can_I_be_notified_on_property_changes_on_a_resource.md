

FAQ How can I be notified on property changes on a resource?
============================================================

It depends what you mean by _properties_. For some metadata stored on resources, such as markers and sync info, an IResourceChangeListener can be used to be notified when they change. Other metadata, such as session and persistent properties, has no corresponding change notification. This is a design trade-off, as tracking and broadcasting change notifications can be quite expensive. Session and persistent properties are designed to be used only by the plug-in that declared the property, so other plug-ins should never be tracking or changing properties declared by your plug-in.

See Also:
---------

*   [FAQ\_How\_can\_I\_be\_notified\_of\_changes\_to\_the\_workspace?](./FAQ_How_can_I_be_notified_of_changes_to_the_workspace.md "FAQ How can I be notified of changes to the workspace?")
*   [FAQ\_How\_do\_I\_store\_extra\_properties\_on\_a_resource?](./FAQ_How_do_I_store_extra_properties_on_a_resource.md "FAQ How do I store extra properties on a resource?")
*   [FAQ\_How\_can\_I\_be\_notified\_when\_the\_workspace\_is\_being_saved?](./FAQ_How_can_I_be_notified_when_the_workspace_is_being_saved.md "FAQ How can I be notified when the workspace is being saved?")
*   [FAQ\_How\_do\_I\_react\_to\_changes\_in\_source_files?](./FAQ_How_do_I_react_to_changes_in_source_files.md "FAQ How do I react to changes in source files?")

