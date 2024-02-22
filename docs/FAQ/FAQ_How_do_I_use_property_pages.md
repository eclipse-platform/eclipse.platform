

FAQ How do I use property pages?
================================

The workbench provides two facilities for presenting the properties of an object: the Property dialog and the Properties view.

The Property dialog is invoked by selecting an object and pressing Alt+Enter or by selecting **File > Properties**. The workbench provides an action, PropertyDialogAction, that you can add to your own view's menu for opening the Property dialog. The Property dialog contains pages contributed by the org.eclipse.ui.propertyPages extension point. Plug-ins can contribute pages in this way for any type of domain object.

The Properties view, also known as the property sheet, is not populated using an extension point but is activated through API, by the PDE editors, or manually, through **Window > Show View**. This view, like the Outline view, asks the active workbench part to contribute its contents. When a part becomes active, the property sheet asks it to adapt to IPropertySheetPage, using the IAdaptable mechanism:

      IWorkbenchPart.getAdapter(IPropertySheetPage.class);

If it wants a completely customized property page, the part can respond to this request and provide its own page. If the part does not provide a page, the property sheet presents a default page that solicits key/value pairs from the active part's selection. This again uses the IAdaptable mechanism to ask the selected element whether it wants to contribute properties. This time it asks the element for an implementation of IPropertySource. The property source is responsible for providing its keys and values, changing values, and restoring default values.

See Also:
---------

*   [FAQ How do I use IAdaptable and IAdapterFactory?](./FAQ_How_do_I_use_IAdaptable_and_IAdapterFactory.md "FAQ How do I use IAdaptable and IAdapterFactory?")
*   [FAQ How do I store extra properties on a resource?](./FAQ_How_do_I_store_extra_properties_on_a_resource.md "FAQ How do I store extra properties on a resource?")
*   Property pages (See **Platform Plug-in Developer's Guide**)
*   Eclipse online articles
    *   ["Take control of your properties"](https://www.eclipse.org/articles/Article-Properties-View/properties-view.html)
    *   ["Simplifying Preference Pages with Field Editors"](https://www.eclipse.org/articles/Article-Field-Editors/field_editors.html)
    *   ["Mutatis mutandis - Using Preference Pages as Property Pages"](https://www.eclipse.org/articles/Article-Mutatis-mutandis/overlay-pages.html)

