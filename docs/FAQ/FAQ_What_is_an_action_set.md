

FAQ What is an action set?
==========================

An action set is a logical group of menus and actions that should appear together at the same time. For example, when you are debugging a Java program, you want all the debug actions, such as **Step** and **Resume**, to appear in the menus. Actions in an action set can appear either in the workbench window toolbar or in the main menus.

You can create your own action sets, thus contributing to the main menu and toolbar, using the org.eclipse.ui.actionSets extension point. Here is an action set definition from the FAQ examples plug-in:

      <extension
            point="org.eclipse.ui.actionSets">
         <actionSet
            label="Sample Action Set"
            visible="false"
            id="org.eclipse.faq.examples.actionSet">
            <menu>...</menu>
            <action>...</action>
            ...
         </actionSet>
      </extension>

The action set declaration itself is followed by a series of menu and action attributes, which are discussed in more detail in the FAQs that follow this one. Action sets are an entirely declarative concept. They cannot be defined, customized, or manipulated programmatically.

See Also:
---------

*   [FAQ How do I make my action set visible?](./FAQ_How_do_I_make_my_action_set_visible.md "FAQ How do I make my action set visible?")
*   [FAQ How do I add actions to the global toolbar?](./FAQ_How_do_I_add_actions_to_the_global_toolbar.md "FAQ How do I add actions to the global toolbar?")
*   [Platform Plug-in Developer Guide](https://help.eclipse.org/help31/index.jsp), under **Reference > Extension Points Reference > org.eclipse.ui.actionSets**
*   ["Contributing Actions to the Eclipse Workbench"](https://www.eclipse.org/articles/article.php?file=Article-action-contribution/index.html)

