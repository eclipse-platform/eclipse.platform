

FAQ How do I use IAdaptable and IAdapterFactory?
================================================

Adapters in Eclipse are generic facilities for mapping objects of one type to objects of another type. This mechanism is used throughout the Eclipse Platform to associate behavior with objects across plug-in boundaries. Suppose that a plug-in defines an object of type _X_, another plug-in wants to create views for displaying _X_ instances, and another wants to extend _X_ to add some extra features. Extra state or behavior could be added to _X_ through subclassing, but that allows for only one dimension of extensibility. In a single-inheritance language, clients would not be able to combine the characteristics of several customized subclasses of _X_ into one object.

Adapters allow you to transform an object of type _X_ into any one of a number of classes that can provide additional state or behavior. The key players are

*   IAdapterFactory, a facility for transforming objects of one type to objects of another type
*   IAdaptable, an object that declares that it can be adapted. This object is typically the input of the adaptation process
*   Adapters, the output of the adaptation process. No concrete type or interface is associated with this output; it can be any object
*   IAdapterManager, the central place where adaptation requests are made and adapter factories are registered
*   PlatformObject, a convenience superclass that provides the standard implementation of the IAdaptable interface

Adapter factories can be registered either programmatically, via the registerAdapters method on IAdapterManager, or declaratively, via the adapters extension point. The only advantage of programmatic registration is that it allows you to withdraw or replace a factory at runtime, whereas the declaratively registered factory cannot be removed or changed at runtime.

Adaptation typically takes place when someone requires an input of a certain type but may obtain an input of a different type, often owing to the presence of an unknown plug-in. For example, WorkbenchLabelProvider is a generic JFace label provider for presenting a model object in a tree or table. The label provider doesn't really care what kind of model object is being displayed; it simply needs to know what label and icon to display for that object. When provided with an input object, WorkbenchLabelProvider adapts the input to IWorkbenchAdapter, an interface that knows how to compute its label and icon. Thus, the adapter mechanism insulates the client-in this case, the label provider-from needing to know the type of the input object.

As another example, the DeleteResourceAction action deletes a selection of resources after asking the user for confirmation. Again, the action doesn't really care what concrete types are in the selection, as long as it can obtain IResource objects corresponding to that selection. The action achieves this by adapting the selected objects to IResource, using the adapter mechanism. This allows another plug-in to reuse these actions in a view that does not contain actual IResource objects but instead contains a different object that can be adapted to IResource.

In all these cases, the code to perform the adaptation is similar. Here is a simplified version of the DeleteResourceAction code that obtains the selected resource for a given input:

      Object input = ...;
      IResource resource = null;
      if (input instanceof IResource) {
         resource = (IResource)input;
      } else if (input instanceof IAdaptable) {
         IAdaptable a = (IAdaptable)input;
         resource = (IResource)a.getAdapter(IResource.class);
      }

Note that it is not strictly necessary for the object being adapted to implement IAdaptable. For an object that does not implement this interface, you can request an adapter by directly calling the adapter manager:

      IAdapterManager manager = Platform.getAdapterManager();
      ... = manager.getAdapter(object, IResource.class);

For an excellent design story on why Eclipse uses adapters, see Chapter 31 of _Contributing to Eclipse_ by Erich Gamma and Kent Beck.

See Also:
---------

*   [FAQ How can I use IWorkbenchAdapter to display my model elements?](./FAQ_How_can_I_use_IWorkbenchAdapter_to_display_my_model_elements.md "FAQ How can I use IWorkbenchAdapter to display my model elements?")

