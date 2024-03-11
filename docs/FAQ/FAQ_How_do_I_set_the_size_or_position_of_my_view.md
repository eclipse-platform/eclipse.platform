

FAQ How do I set the size or position of my view?
=================================================

The short answer is that you can't always control the size and position of your view. If you create your own perspective, you can specify the initial size and position of the view relative to other views. Once the perspective has been opened, the user is free to move the view elsewhere and change its size. At this point, you no longer have control over the view's size and position.

  
In a similar manner, you can influence the initial size and position of your view in other perspectives by using the perspectiveExtensions extension point. As with perspectives, this extension point allows you to specify the size and position of your view relative to other views that are known to be in that perspective. Note that you shouldn't generally make your view appear by default in perspectives provided by other plug-ins. If many plug-ins did this, the host perspective would quickly become cluttered and difficult to use. To ensure that your view does not appear by default, specify the attribute visible="false" in your extension declaration.

  

See Also:
---------

[FAQ How do I create a new perspective?](./FAQ_How_do_I_create_a_new_perspective.md "FAQ How do I create a new perspective?")

[FAQ How can I add my views and actions to an existing perspective?](./FAQ_How_can_I_add_my_views_and_actions_to_an_existing_perspective.md "FAQ How can I add my views and actions to an existing perspective?")

[FAQ Why can't I control when, where, and how my view is presented?](./FAQ_Why_cant_I_control_when_where_and_how_my_view_is_presented.md "FAQ Why can't I control when, where, and how my view is presented?")

