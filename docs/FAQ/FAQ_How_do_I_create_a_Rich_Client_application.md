

FAQ How do I create a Rich Client application?
==============================================

An Eclipse RCP application has full control over how the user interface is created. The locus of control for an RCP application-the place where all configuration starts-is the WorkbenchAdvisor class. Your subclass of WorkbenchAdvisor controls the initial layout and appearance of the workbench window, as well as what commands appear in the menus and toolbars. Here is an example of a bare-bones RCP application:

   public class MinimalRCPApp extends WorkbenchAdvisor
      implements IPlatformRunnable {
      public String getInitialWindowPerspectiveId() {
         return "org.eclipse.faq.minimalperspective";
      }
      public void preWindowOpen(
                     IWorkbenchWindowConfigurer wwc) {
         configurer.setShowMenuBar(false);
         configurer.setShowFastViewBars(false);
         configurer.setShowStatusLine(false);
         configurer.setShowCoolBar(false);
      }
      public Object run(Object args) throws Exception {
         Display d = PlatformUI.createDisplay();
         int ret = PlatformUI.createAndRunWorkbench(d, this);
         if (ret == PlatformUI.RETURN_RESTART)
            return EXIT_RESTART;
         return EXIT_OK;
      }
   }

  
This application creates a blank workbench window with no toolbars, no menus, no status line, and no views or editors (Figure 13.1). The application will run until the user closes the workbench window.

    <img src=../images/minimal_app.png>

    **Figure 13.1**   Minimal RCP application

  

The application's run method is the one we've seen in previous application examples. You need to specify the name of the class with this method when declaring your application in the plugin.xml file. This example creates a workbench and runs the event loop by calling createAndRunWorkbench. The preWindowOpen method is your opportunity to customize the basic appearance of the window. Finally, the getInitialWindowPerspectiveId method must specify the ID of the initial perspective to be displayed.

  
That's all there is to it! The rest of an RCP application is developed just like any other plug-in. You need to create one or more perspectives and populate them with the views and editors that apply for your application. These are created by using the standard org.eclipse.ui extension points, all of which are available in a custom application.

  
A binary download of the RCP can be obtained for any Eclipse build from the eclipse.org downloads page. This download does not contain an application, but it can be set as the target platform for your own RCP application from the **Plug-in Development >** Target Platform **preference page.**

  

See Also:
---------

[FAQ\_How\_do\_I\_create\_a\_new_perspective?](./FAQ_How_do_I_create_a_new_perspective.md "FAQ How do I create a new perspective?")

[FAQ\_How\_do\_I\_create\_an\_application?](./FAQ_How_do_I_create_an_application.md "FAQ How do I create an application?")

