

FAQ How do I dynamically register an editor to handle a given extension?
========================================================================

You can't. Editors, like most other extensions to the platform, must be specified declaratively in the plug-in manifest file. You cannot dynamically install a new editor except by dynamically installing a new plug-in containing the new editor.

  
The only thing you _can_ currently do programmatically is specify the default editor to use for a given file name or extension. The editor must already be registered with the platform through a plug-in and must already declare that it supports files with that name or extension.

Here is an example snippet that sets the default editor for text files to be the built-in platform text editor:

        IEditorRegistry registry = 
                PlatformUI.getWorkbench().getEditorRegistry();
        registry.setDefaultEditor("*.txt", 
                "org.eclipse.ui.DefaultTextEditor");

  

See Also:
---------

[FAQ How do I create my own editor?](./FAQ_How_do_I_create_my_own_editor.md "FAQ How do I create my own editor?")

