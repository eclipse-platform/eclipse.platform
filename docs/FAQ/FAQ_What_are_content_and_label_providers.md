

FAQ What are content and label providers?
=========================================

Minimally, all content viewers require you to supply a content provider and the label provider to interact with your model. The content provider is the viewer's gateway to the structure of the model elements that will be displayed in the viewer. When an input is provided to a viewer, the viewer asks the content provider what elements to show for that input. For more complex structures, such as trees, the viewer asks the content provider when it needs to know the children or parent of a given model element.

In addition to answering these questions from the viewer, the content provider must also notify the viewer whenever the model has been changed. The content provider does this by calling the various incremental update methods on the viewer subclasses. Although these methods look slightly different, depending on the kind of viewer, they generally follow a naming convention:

*   add, for adding a single new element or batches of new elements.
*   remove, for removing single elements or batches of elements.
*   update, for updating the appearance of a single item, such as the label or icon. Optionally, this method takes a list of properties that are passed to the label provider, filters, and sorters to determine whether they are affected by the change.
*   refresh for naive updating of an element and its children.

Although refresh is the easiest of these methods to use, it is also generally the least efficient. Because it is provided with no context information to figure out exactly what has changed, it simply rebuilds the view from scratch, starting with the provided element. This can in turn trigger costly sorting and filtering of the elements.

The label provider deals only with presentation of individual model elements. The viewer asks the label provider only what text and what icon, if any, are associated with each model element? The label provider is also responsible for disposing of any images it creates when the viewer is disposed of.

See Also:
---------

*   [FAQ What is a viewer?](./FAQ_What_is_a_viewer.md "FAQ What is a viewer?")
*   [FAQ Why should I use a viewer?](./FAQ_Why_should_I_use_a_viewer.md "FAQ Why should I use a viewer?")
*   [FAQ How do I use properties to optimize a viewer?](./FAQ_How_do_I_use_properties_to_optimize_a_viewer.md "FAQ How do I use properties to optimize a viewer?")

