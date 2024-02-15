

FAQ Can I create an application that doesn't have views or editors?
===================================================================

When you create an RCP application using a workbench advisor, you are still constrained to using the standard Eclipse UI constructs of views, editors, and perspectives. If you have very different UI requirements but still want to use the plug-in infrastructure, you can always create an application built on only SWT and JFace. This has been possible since the first release of Eclipse as SWT and JFace have no other dependencies outside the base runtime plug-in. JFace provides a basic ApplicationWindow with optional menus, toolbar, and status line.

  
Configuration of a JFace application works through subclassing rather than plugging in an advisor. Your application needs to subclass the JFace class called ApplicationWindow and override the various methods that are used to customize the appearance and behavior of the window. The following is a simple JFace application window from the FAQ examples plug-in. As with other applications, you begin by creating a class that implements IPlatformRunnable:

      public class JFaceApp implements IPlatformRunnable {
         public Object run(Object args) throws Exception {
            Display display = new Display();
            JFaceAppWindow window = new JFaceAppWindow();
            window.open();
            Shell shell = window.getShell();
            while (!shell.isDisposed()) {
               if (!display.readAndDispatch())
                  display.sleep();
            }
            return EXIT_OK;
         }
      }

JFaceAppWindow is a subclass of the framework class ApplicationWindow. The subclass creates a simple window with a menu bar, a status line, and a single button inside the main window that is used to exit the application (Figure 16.2).

Complete source for the class can be found in the FAQ Examples plug-in, but here is the basic structure:

      public class JFaceAppWindow extends ApplicationWindow {
         public JFaceAppWindow() {
            super(null);
            addMenuBar();
            addStatusLine();
         }
         protected void configureShell(Shell shell) {
            super.configureShell(shell);
            shell.setText("Simple JFace Application");
         }
         ...
      }

The subclass also needs to override the createContents method to create the SWT widgets that will appear in the window's main content area. Override createMenuManager to populate the window's menus, createToolBarManager to populate the toolbar, and so on. If you browse through the ApplicationWindow class, you will see that many other hook methods allow your application to customize the appearance of the top-level window.

  

See Also:
---------

[FAQ How do I build menus and toolbars programmatically?](./FAQ_How_do_I_build_menus_and_toolbars_programmatically.md "FAQ How do I build menus and toolbars programmatically?")

