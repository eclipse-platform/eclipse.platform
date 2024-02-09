

FAQ How do I provide F1 help?
=============================

When the user presses F1, context-sensitive help is displayed that briefly describes the action or widget that is currently in focus and provides links to related help topics. You can implement this form of help for your plug-in by associating a _help context ID_ with the actions, menus, and controls in your plug-in. Help is designed in such a way that disruption in the code is kept to an absolute minimum. The help text and links are provided via an extension point and don't even need to reside in the same plug-in as the code that the help text refers to. This makes it easy to make help an optional part of your plug-in or to provide help in various languages.

  
The only code required to associate help with your UI component is to link a help context ID to it. This is accomplished by using the various setHelp methods on the WorkbenchHelp class. For example, adding help to a view can be done in the view's createPartControl method:

      public void createPartControl(Composite parent) {
         ...
         PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, 
            "org.eclipse.faq.examples.books_view");
      }

The setHelp methods can be used to add help contexts to actions, menus, and arbitrary controls. Because context-sensitive help operates on the control that is in focus, it makes sense to associate help contexts only with controls that are able to take focus. See the help documentation for more details.

For actions that are contributed declaratively, help contexts are contributed in the XML action definition. For example, for an action in an actionSet, the context is specified using the helpContextId attribute:

      <action
         ...
         helpContextId="org.eclipse.faq.examples.console_action"
      </action>

Help contexts are specified declaratively for the following extension points in the Eclipse SDK:

*   org.eclipse.ui.actionSets
*   org.eclipse.ui.editorActions
*   org.eclipse.ui.popupMenus
*   org.eclipse.ui.viewActions
*   org.eclipse.ui.ide.markerHelp
*   org.eclipse.search.searchPages
*   org.eclipse.debug.ui.launchShortcuts
*   org.eclipse.debug.ui.launchConfigurationTabGroups

  

See Also:
---------

[FAQ\_Where\_do\_I\_get_help?](./FAQ_Where_do_I_get_help.md "FAQ Where do I get help?")

[FAQ\_How\_do\_I\_contribute\_help\_contexts?](./FAQ_How_do_I_contribute_help_contexts.md "FAQ How do I contribute help contexts?")

