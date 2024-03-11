

FAQ How do I prompt the user to select a resource?
==================================================

Several dialogs are available for prompting the user to select one or more resources in the workspace. Each dialog has its own particular attributes and uses, and supports varying degrees of customization based on your needs. Note that you can change the title of many of these dialogs by using the method SelectionDialog.setTitle to suit your particular application. Here is a summary of the available dialogs:

*   ContainerSelectionDialog prompts the user to select a single project

or folder in the workspace. This dialog be configured to allow the user to specify a folder that does not yet exist. </li>

*   ElementListSelectionDialog is a powerful generic

selection dialog that is widely used throughout the workbench. This dialog has a text box at the top that allows the user to enter a pattern. As the user types, the list below narrows down to show only the matching elements. The input elements can be any kind of objects. </li>

*   TwoPaneElementSelector is much like

ElementListSelectionDialog, except an extra qualifier list is added at the bottom. When a match is selected in the middle pane, the corresponding qualifiers are shown in the bottom pane. This is used most prominently by the **Open Type** and **Open** Resource **actions. In these dialogs, the qualifier is either the package or** folder name for the resource that is selected in the middle pane. </li>

*   NewFolderDialog prompts the user to enter the name of a new

folder directly below a supplied parent container. This dialog allows the user to create a linked folder that maps to a directory in the file system outside the project content area. </li>

*   ResourceListSelectionDialog prompts the user to select a single

resource from a flat list of resource names. This dialog can be configured with a resource type mask to narrow the list to only folders, only files, or both. The user can enter a filter pattern to narrow down the list of resources. Filtering and populating the dialog are done in a background thread to ensure responsiveness in large workspaces. This dialog is designed to be subclassed to allow for further customization. </li>

*   ResourceSelectionDialog prompts the user to select one or

more resources below a given root. This dialog displays a table tree of containers on the left-hand side and a list of resources on the right-hand side, just like the panes at the top of the file system export wizard. </li>

*   FileSelectionDialog is deprecated in Eclipse 3.0. Its

intent was to provide a generic dialog to allow the user to make selections from file-system-like structures such as the workspace, zip files, or the actual file system. Most of the functionality of this dialog can be found in other dialogs. </li>

*   SaveAsDialog has hard-coded

title and messages; otherwise, it could be used in any context requiring 

a file selection. The user can supply the name of a new or existing file. This dialog is the file equivalent of ContainerSelectionDialog. The dialog returns an IPath result and does not check whether the file or parent folders already exist. </li>

  

The FAQ Examples plug-in for this book includes an action called ResourceSelectionAction that demonstrates the use of all these dialogs.

  

See Also:
---------

[FAQ\_How\_do\_I\_prompt\_the\_user\_to\_select\_a\_file\_or\_a_directory?](./FAQ_How_do_I_prompt_the_user_to_select_a_file_or_a_directory.md "FAQ How do I prompt the user to select a file or a directory?")

