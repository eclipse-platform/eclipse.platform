

FAQ How do I contribute help contexts?
======================================

  

Help contexts are added by using the org.eclipse.help.contexts extension point. Contexts are usually specified by using a separate help plug-in, making it easier to switch help content for various languages. The context extension specifies the path of a separate XML file where the data is stored:

      <extension point="org.eclipse.help.contexts">
         <contexts
            file="help_contexts.xml"
            plugin="org.eclipse.faq.examples">
         </contexts>
      </extension>

The contexts file includes a description for each context and can, optionally, add links to HTML help content files in the plug-in. See the help documentation for more details on the format of help context files.

See Also:
---------

[FAQ\_How\_do\_I\_provide\_F1\_help?](./FAQ_How_do_I_provide_F1_help.md "FAQ How do I provide F1 help?")

Go to **Platform Plug-in Developer Guide > Programmer's** Guide > Plugging in help**.**

