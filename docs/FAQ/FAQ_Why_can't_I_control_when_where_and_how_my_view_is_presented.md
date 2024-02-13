

FAQ Why can't I control when, where, and how my view is presented?
==================================================================

Plug-in writers implementing views are often frustrated by their lack of control over when and how their views are presented to the user. Common questions include the following:

*   How do I control when users can open my view?</li>

*   How do I ensure that my two views are always open at the same time?</li>

*   How do I make a view appear in a floating shell or dialog?</li>

*   How can I ensure that my view is a certain size?</li>

The answer to such questions is more philosophical than some plug-in writers would like.

As an integration platform, Eclipse must balance between customizability and conformity. The platform needs to be customizable in order to adapt to the unforeseeable needs of plug-ins being integrated into the platform. On the other hand, in order to provide a coherent user experience, components need to be presented consistently. The ultimate goal is for the user to perceive the end product as a coherent and self-consistent application rather than as a collection of isolated components that don't know anything about one another.

  
The platform seeks this balance with views by giving the view implementer limited control over how views are presented. The view writer has control over the body of the view but little or no control over where the view appears in the workbench page, what size it has, and when it is opened or closed. This approach prevents individual views from exerting too much control over the rest of the workbench window. Because the view implementer can never foresee exactly what configuration of views and editors the end user may want to have open, it cannot make reasonable choices about what happens beyond its borders. Only users are in a position to know exactly what kind of layout they want.

  
Giving limited power to individual views also gives the platform the flexibility to change its look and feel between releases without breaking API compatibility. For example, version 1.0 of the platform allowed views to float as separate shells outside the workbench window, but this capability was removed in version 2.0. The API designers expose only functionality that they are confident can be supported for the long term.

  

See Also:
---------

[FAQ How do I create fixed views and perspectives?](./FAQ_How_do_I_create_fixed_views_and_perspectives.md "FAQ How do I create fixed views and perspectives?")

