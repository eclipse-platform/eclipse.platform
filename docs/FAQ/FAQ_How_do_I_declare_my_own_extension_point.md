

FAQ How do I declare my own extension point?
============================================

Declaring an extension point is arguably one of the more difficult and less documented and supported tasks in Eclipse. Most plug-in writers simply copy an extension point schema from others to get an approximation of what they need.

Adding an extension point can be done with some PDE support by following these steps.

*   Edit your plugin.xml using the Manifest Editor. Select the **Extension Points** tab, and click **Add...**.
*   Now stop for a minute. Think up a good name and an equally descriptive ID for your extension point. Don't choose too lightly. These two names are part of your plug-in API. The ID is one word without dots; your plug-in ID will be prepended to it. When in doubt, press F1 to get help.
*   Choose a name for your schema. This file name is private to you, so you can choose any name you like. Check the box to edit the schema. Click **Finish** to launch the PDE Schema Editor to edit the schema.
*   Click **New Element**; in the properties view, change the name to the extension element you want your clients to fill in.
*   Add a new attribute to this element, and call it **id**. Change the value of **Use** to **Required**. In the Properties view, click the **Clone this attribute** button in the local toolbar.
*   Rename the clone to **name**. Clone it.
*   Rename the second clone to **class**. Change the **kind** to **java**.
*   Choose the interface your contributors need to implement or the class they need to subclass. Note that it is generally better to provide a superclass, as this allows you greater flexibility in extending the API in the future without breaking existing implementations. Choose a descriptive name that captures the meaning of the collaboration between you and your contributors.
*   In the **Based On** property, enter the name of the interface/class to create an executable extension from.

That's it. You now have defined a new extension that others can contribute to. Look around in the editor and add descriptions, documentation, and example code where appropriate.

If you want to customize the extension with more attributes, read **Help > Help Contents... > PDE Guide > Extension Point Schema > Extension point schema editor**.

See Also:
---------

*   [FAQ What are extensions and extension points?](./FAQ_What_are_extensions_and_extension_points.md "FAQ What are extensions and extension points?")
*   [FAQ What is an extension point schema?](./FAQ_What_is_an_extension_point_schema.md "FAQ What is an extension point schema?")
*   [FAQ How do I find all the plug-ins that contribute to my extension point?](./FAQ_How_do_I_find_all_the_plug-ins_that_contribute_to_my_extension_point.md "FAQ How do I find all the plug-ins that contribute to my extension point?")

