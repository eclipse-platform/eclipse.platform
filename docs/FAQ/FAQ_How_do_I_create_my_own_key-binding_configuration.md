

FAQ How do I create my own key-binding configuration?
=====================================================

A keyboard shortcut (key binding) defines a key that when pressed will execute an Eclipse command. Keyboard shortcuts are organized into independent sets called key configurations or schemes. The user chooses the key configuration that emulates the shortcut keys of his or her favorite editor or IDE.

To see the current key configuration and its keyboard shortcuts, choose the **Window > Preferences** menu command to open the Eclipse workbench **Preferences**. Select the **General > Editor > Keys** page. This page displays the currently active **Scheme** (key configuration) and the keyboard shortcuts it defines. **Emacs** is a set of keyboard shortcuts that emulates emacs. Similarly, **Microsoft Visual Studio** defines a set of shortcuts that emulates that IDE. The native configuration for Eclipse is **Default**.

There are no APIs for defining key-binding configurations programmatically, but you can create them in a plug-in by using the org.eclipse.ui.commands extension point. First, you need to define your new configuration:

      <keyConfiguration
         name="My Configuration"
         parent="org.eclipse.ui.defaultAcceleratorConfiguration"
         description="This is a simple configuration"
         id="org.eclipse.faq.sampleConfiguration">
      </keyConfiguration>

By specifying a parent, you are saying that your configuration should inherit key bindings from the parent unless they are explicitly set in your configuration. When key bindings are defined, they will refer to the configuration they belong to. If you write your own configuration, you'll also need to define new key bindings for all the commands that you want to belong to your configuration.

See Also:
---------

*   [FAQ How do I provide a keyboard shortcut for my action?](./FAQ_How_do_I_provide_a_keyboard_shortcut_for_my_action.md "FAQ How do I provide a keyboard shortcut for my action?")

