

FAQ How do I add label decorations to my viewer?
================================================

Suppose that your viewer contains model elements for which other plug-ins have defined label decorators. To make those decorations appear in your viewer, you need to install a decorating label provider. Assuming that you have already written your own basic label provider, simply do the following to add declarative decorations from other plug-ins:

      ILabelProvider lp = ... // your basic label provider implementing ILabelProvider
      ILabelDecorator decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
      viewer.setLabelProvider(new DecoratingLabelProvider(lp, decorator));

See Also:
---------

*   [FAQ What is a label decorator?](./FAQ_What_is_a_label_decorator.md "FAQ What is a label decorator?")
*   [FAQ How do I create a label decorator declaratively?](./FAQ_How_do_I_create_a_label_decorator_declaratively.md "FAQ How do I create a label decorator declaratively?")

