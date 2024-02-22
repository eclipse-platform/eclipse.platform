

FAQ How do I provide a keyboard shortcut for my action?
=======================================================

A keyboard shortcut (key binding) defines a key that when pressed will execute an Eclipse command. Keyboard shortcuts are organized into independent sets called key configurations or schemes. The user chooses the key configuration that emulates the shortcut keys of his or her favorite editor or IDE.

This FAQ describes two different ways to define shortcut keys.

Display and Edit the Current Keyboard Shortcuts
-----------------------------------------------

To see the current key configuration and its keyboard shortcuts, choose the **Eclipse > Preferences** menu command to open the Eclipse workbench **Preferences**. Select the **General > Editor > Keys** page. This page displays the currently active **Scheme** (key configuration) and the keyboard shortcuts it defines. **Emacs** is a set of keyboard shortcuts that emulates emacs. Similarly, **Microsoft Visual Studio** defines a set of shortcuts that emulates that IDE. The native configuration for Eclipse is **Default**.

Define a Keyboard Shortcut Through an Extension Point
-----------------------------------------------------

Keyboard shortcuts can also be created by defining a _key sequence_, using the org.eclipse.ui.bindings extension point. When you define a key sequence, you generally specify four things:

*   The context, or scope, for the key binding. For example, the text editor defines a context that can override bindings from the global context.
*   The scheme, for the key binding, for example, the default key binding scheme or the Emacs key binding scheme.
*   The ID of the command that you are creating a binding for. You can find command IDs by browsing the plugin.xml file of the plug-in that defines that action.
*   The accelerator sequence.

You can also define a key binding that applies only to a particular locale or platform. For example, you can define an accelerator that applies only to a German Linux GTK installation of Eclipse. See the command extension point documentation for more details on these advanced features.

The following is an example key-binding definition. 
This binding sets up a toggle comment accelerator for a hypothetical AMPLE language editor, which by default has no key binding:

		<extension point="org.eclipse.ui.bindings">
			<key sequence="Ctrl+7"
				commandId="uk.co.example.actions.togglecomment"
				schemeId="org.eclipse.ui.defaultAcceleratorConfiguration"
				contextId="uk.co.example.ampleEditorScope"/>
		</extension>

The difference between a scheme and a context can be confusing at first. The scheme is explicitly set by the user; once it is set, it does not change. The context can be changed programmatically by any plug-in. For example, you can change the context whenever your view or editor becomes active, using AbstractTextEditor.setKeyBindingScopes().

  

See Also:
---------

*   [FAQ How do I create my own key-binding configuration?](./FAQ_How_do_I_create_my_own_key-binding_configuration.md "FAQ How do I create my own key-binding configuration?")

