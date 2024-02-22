

FAQ What is a label decorator?
==============================

A DecoratingLabelProvider can be used when an element's text and image need to be annotated to show different states or properties. This is accomplished by bringing together a standard label provider with an optional decorator. The standard label provider is first asked to generate the text and image for the input element, and then the decorator, if installed, is allowed to augment or replace the original text and image with new values. JFace provides a helper class, CompositeImageDescriptor, to help you combine an image with one or more overlays. Because multiple overlays in a particular position will obscure one another, you must be careful to avoid overlapping decorators when creating a decorating label provider.

Lightweight label decorators were introduced in Eclipse 2.1. These decorators abstract away the details of performing the image overlay. An implementation of ILightWeightLabelDecorator is provided with an IDecoration object, which it calls in order to add the decorations. The following simple example decorates a viewer containing java.io.File objects to indicate whether they are read-only:

      ImageDescriptor readOnlyOverlay = ...;
      public void decorate(Object element, IDecoration decoration) {
         if (!(element instanceof java.io.File))
            return;
         boolean readOnly = !((java.io.File) element).canWrite();
         if (!readOnly)
            return;
         decoration.addOverlay(readOnlyOverlay);
         decoration.addSuffix(" (read only)");
      }

A decorating label provider is installed on a viewer in the same way as any other label provider:

      ILabelProvider lp = ... the basic label provider
      ILabelDecorator decorator = ... the decorator
      viewer.setLabelProvider(new DecoratingLabelProvider(lp, decorator));

See Also:
---------

*   [FAQ What is a viewer?](./FAQ_What_is_a_viewer.md "FAQ What is a viewer?")
*   [FAQ What are content and label providers?](./FAQ_What_are_content_and_label_providers.md "FAQ What are content and label providers?")
*   [FAQ How do I create a label decorator declaratively?](./FAQ_How_do_I_create_a_label_decorator_declaratively.md "FAQ How do I create a label decorator declaratively?")
*   [FAQ How do I add label decorations to my viewer?](./FAQ_How_do_I_add_label_decorations_to_my_viewer.md "FAQ How do I add label decorations to my viewer?")

