

FAQ How do I use the preference service?
========================================

In addition to the plug-in preferences for the local workspace obtained via Plugin.getPluginPreferences, a preference service new in Eclipse 3.0 can be used to store preferences in different places. This facility is similar to the Java 1.4 preferences API. Each preference object is a node in a global preference tree. The root of this preference tree is accessed by using the getRootNode method on IPreferenceService. The children of the root node represent different scopes. Each scope subtree chooses how and where it is persisted: whether on disk, in a database, or not at all. Below the root of each scope are typically one or more levels of contexts before preferences are found. The number and format of these contexts depend on the given scope. For example, the instance and default scopes use a plug-in ID as a qualifier. The project scope uses two qualifiers: the project name and the plug-in ID. Thus, the fully qualified path of a preference node for the FAQ Examples plug-in in the workspace project My Project would be

      /project/My Project/org.eclipse.faq.examples

Below the level of scopes and scope contexts, the preference tree can have further children for storing hierarchies of information. This is analogous to the org.eclipse.ui.IMemento facility used for persisting user-interface states. With the new preference mechanism, it is very easy to create hierarchies of preference nodes for storing hierarchical preference data.

Lookups in this global preference tree can be done in a number of ways. If you know exactly which scope and context you are looking for, you can start at the root node and navigate downward using the node methods. Each scope typically also provides a public class for obtaining preferences within that scope. For example, you can create an instance of ProjectScope on an IProject to obtain preferences stored in that project. Finally, the preference service has methods for doing preference lookups that search through all scopes. This can be used when you allow users to specify what scope their preferences are stored at and you want more local scopes to override values in more global scopes. For example, JDT compiler preferences can be specified at the project scope if you want to override preferences for a given project. They can also be stored at the instance scope to specify preferences that apply to all projects in a workspace.

See Also:
---------

*   [FAQ\_What\_is\_a\_preference_scope?](./FAQ_What_is_a_preference_scope.md "FAQ What is a preference scope?")

