

FAQ What is an Eclipse application?
===================================

Technically, an Eclipse application is a plug-in that creates an extension for the extension point org.eclipse.core.runtime.applications. However, the extension point is fairly special. Only one application gets to run in a given Eclipse instance. This application is specified either on the command line or by the primary feature. After the platform starts up, control of the VM's main thread is handed over to the application's run method. The application's entire lifecycle occurs within the scope of this method. When the run method returns, the platform shuts down.

The application is essentially the boss; it's the Eclipse analog of the C or Java main method. All other plug-ins in the configuration plug into the application. What goes into the run method is entirely up to you. It can be a graphical application, which will create a user interface and run some kind of event loop, or a completely headless application that runs without interacting with a user.

Because a running Eclipse instance has only one application in it, the philosophy of building applications is very different from the approach when building plug-ins. Essentially, the flexibility given to plug-ins must be mitigated by the fact that other plug-ins in the system may have competing requirements. The laws of plug-in behavior are designed to allow plug-ins to interact in ways that do not impinge on the behavior of other plug-ins. Such constraints are not as important for the application, which can have the final say when the needs of various plug-ins don't intersect. Whereas plug-ins are citizens of the Eclipse Platform, the application is king. For example, because the application is always started first, the lazy-loading principle doesn't apply to it. The application can customize the menus and toolbars programmatically rather than using the various workbench extension points. The application can also determine whether views and editors have title bars and whether views can be closed or resized.

The Eclipse SDK is one particularly well-known example of an Eclipse application. To explore how it works, start by looking at the IDEApplication class in the org.eclipse.ui.ide plug-in.

  

See Also:
---------

[FAQ What is the difference between a product and an application?](./FAQ_What_is_the_difference_between_a_product_and_an_application.md "FAQ What is the difference between a product and an application?")

  

