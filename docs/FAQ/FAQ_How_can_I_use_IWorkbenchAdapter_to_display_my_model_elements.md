

FAQ How can I use IWorkbenchAdapter to display my model elements?
=================================================================

The IAdaptable mechanism in Eclipse can be used to add visual presentation to your model objects without introducing UI code in your model layer. This follows the layering principle followed in the Eclipse Platform, where core code has no dependency on UI code. To do this, your model objects must implement the IAdaptable interface. This is typically achieved by simply subclassing PlatformObject, but if that's not possible, you can implement the interface directly. See the javadoc of PlatformObject for more details.

In your UI layer, you need to register an adapter factory that can return an implementation of IWorbenchAdapter for your model objects. If your adapter doesn't need to maintain any state, it's a good idea to make it a singleton to avoid creating extra objects for each model element. See the class WorkbenchAdapterFactory in the org.eclipse.ui.ide plug-in for an example of an adapter factory that creates IWorkbenchAdapter instances for IResource objects.

Once you have defined and registered such a factory, you can simply use WorkbenchContentProvider and WorkbenchLabelProvider in any tree or table viewer that contains your model objects. These special providers delegate their implementations to the underlying model objects by asking their IWorkbenchAdapter to compute the label or children of the elements.

See Also:
---------

*   [FAQ How do I use IAdaptable and IAdapterFactory?](./FAQ_How_do_I_use_IAdaptable_and_IAdapterFactory.md "FAQ How do I use IAdaptable and IAdapterFactory?")

