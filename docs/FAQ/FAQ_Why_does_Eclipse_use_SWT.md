

FAQ Why does Eclipse use SWT?
=============================

IBM's first-generation Java development environment, VisualAge for Java, was written in Smalltalk, using an in-house implementation of the language and an in-house virtual machine and widget toolkit. The purpose of the Smalltalk widget toolkit, called Common Widgets (CW), was to provide a thin set of common APIs for building Smalltalk GUIs that run on a broad variety of platforms, implemented using the native widgets available on each platform.

When the decision was made in 1998 to use Java as the implementation language for the next generation of tools, the brand new Swing toolkit was initially evaluated as a GUI toolkit. However, the design philosophy of Swing was based on a strategy of implementing widgets in Java rather than leveraging the native widgets provided by each platform. Based on their experience with Smalltalk, the Eclipse development team believed that native look, feel, and performance were critical to building desktop tools that would appeal to demanding developers. As a result, the team applied the technology they had built for Smalltalk to build SWT-a platform-independent widget API for Java implemented using native widgets.

As this Java tooling framework evolved into what is now called Eclipse, new reasons emerged for choosing SWT as the widget set. Eclipse was designed from the start as an integration platform; a fundamental goal was to provide a platform that integrated seamlessly with other user applications. SWT, with its native widgets, was a natural choice. SWT applications look and respond like native apps, and have tight integration with such operating system features as the clipboard, drag-and-drop, and ActiveX controls on Windows. To this day, many people getting their first glimpse of an Eclipse-based product don't believe that it is written in Java. It is difficult to differentiate from, and smoothly integrates with, other native applications on each of its target operating systems. The question then becomes, Why _shouldn't_ Eclipse use SWT?

A major downside of SWT in the past was that it was not very compatible with Swing-based applications. This was a strike against Eclipse's primary goal as a tool-integration platform as companies with existing Swing-based applications were faced with the extra overhead of porting to SWT if they wanted to integrate cleanly with Eclipse. This was exacerbated by the fact that, as a new technology, few skilled SWT developers were in the market, and companies were reluctant to bet their tooling strategy on such an unknown quantity. As SWT has gained popularity, building a large community of developers and improved training and support, this has become less of an issue. In Eclipse 3.0, the final hurdle is coming down. There is now support for [interoperability between SWT and Swing](./FAQ_How_do_I_embed_AWT_and_Swing_inside_SWT.md "FAQ How do I embed AWT and Swing inside SWT?").

See Also:
---------

*   [FAQ How do I embed AWT and Swing inside SWT?](./FAQ_How_do_I_embed_AWT_and_Swing_inside_SWT.md "FAQ How do I embed AWT and Swing inside SWT?")

