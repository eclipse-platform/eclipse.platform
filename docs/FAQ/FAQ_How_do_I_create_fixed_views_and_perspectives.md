

FAQ How do I create fixed views and perspectives?
=================================================

New APIs in Eclipse 3.0 allow perspectives more control over how their views are presented. These APIs are useful in RCP applications that want different view-presentation models. The IPageLayout interface, provided when your perspective factory is creating its initial layout, has methods for customizing how views will be presented in that perspective. The setFixed method on IPageLayout indicates that a perspective should be _fixed_. In a fixed perspective, views and editors cannot be moved or zoomed by the user.

A _stand-alone view_, created with the method addStandaloneView, cannot be stacked together with other views and can optionally hide its title bar. A view with its title bar hidden cannot be closed, minimized, or moved. For further control over whether views can be closed or moved, you can obtain an IViewLayout instance for any view in the perspective. Following is an example of a fixed perspective that creates a stand-alone view above the editor area that cannot be moved or closed.

      class RecipePerspective implements IPerspectiveFactory {
         public void createInitialLayout(IPageLayout page) {
            page.setEditorAreaVisible(true);
            page.setFixed(true);
            page.addStandaloneView(
               RecipePlugin.VIEW_CATEGORIES, 
               false, IPageLayout.TOP, 0.2f, 
               IPageLayout.ID\_EDITOR\_AREA);
            IViewLayout view = page.getViewLayout(
               RecipePlugin.VIEW_CATEGORIES);
            view.setCloseable(false);
            view.setMoveable(false);
         }
      }

You can add fixed and stand-alone views to perspectives from other plug-ins using the perspectiveExtensions extension point. See the extension point documentation for more details.

  

See Also:
---------

[FAQ How can I add my views and actions to an existing perspective?](./FAQ_How_can_I_add_my_views_and_actions_to_an_existing_perspective.md "FAQ How can I add my views and actions to an existing perspective?")

[FAQ Why can't I control when, where, and how my view is presented?](./FAQ_Why_cant_I_control_when_where_and_how_my_view_is_presented.md "FAQ Why can't I control when, where, and how my view is presented?")

