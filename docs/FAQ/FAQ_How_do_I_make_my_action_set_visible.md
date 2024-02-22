

FAQ How do I make my action set visible?
========================================

Simply defining an action set does not guarantee that it will appear in the UI. This characteristic is very important, as an Eclipse product with thousands of plug-ins would quickly become overwhelmed with actions if their appearance in the UI was not carefully controlled. An action set can be made visible in a number of ways:

*   Set the visible attribute to true in the action set declaration. This will add your action set unconditionally to all perspectives. You should _almost never_ do this in a real application unless you are certain that your actions are needed all the time in all perspectives. Keep in mind the scalability problems with using this approach.
*   Define a perspective or perspective extension. This will limit the appearance of your action set to a specified set of perspectives.
*   Define an action set part association, using the extension point org.eclipse.ui.actionSetPartAssociations. This extension links an action set to one or more views and editors. The action set will appear only when one of those parts is visible.
*   Finally, the user can always have the last say by customizing perspectives (**Window > Customize Perspective**). From here, the user can turn on or off any action sets for the current perspective. This will override all the other mechanisms for defining action set visibility.

See Also:
---------

*   [FAQ How can I add my views and actions to an existing perspective?](./FAQ_How_can_I_add_my_views_and_actions_to_an_existing_perspective.md "FAQ How can I add my views and actions to an existing perspective?")

