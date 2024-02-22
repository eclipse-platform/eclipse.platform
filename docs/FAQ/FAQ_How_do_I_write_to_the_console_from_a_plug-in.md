FAQ How do I write to the console from a plug-in?
=================================================

Many of the people asking this question are confused by the fact that two Eclipse instances are in use when you are developing plug-ins. One is the _development_ platform you are using as your IDE, and the other is the _target_ platform-also known as the runtime workbench-consisting of the plug-ins in the development workbench you are testing against. When a plug-in in the target platform writes a message to System.out or System.err, the message appears in the Console view of the development platform. This view emulates the Java console that appears when Eclipse runs under Windows with java.exe. You should be writing to the console only in this manner when in debug mode (see [FAQ\_How\_do\_I\_use\_the\_platform\_debug\_tracing_facility?](./FAQ_How_do_I_use_the_platform_debug_tracing_facility.md "FAQ How do I use the platform debug tracing facility?")).

  
In some situations however, a plug-in in the development platform has a legitimate reason to write to the development platform Console view. Some tools originally designed for the command line, such as Ant and CVS, traditionally use console output as a way of communicating results to the tool user. When these tools are ported for use with an IDE, this console output is typically replaced with richer forms of feedback, such as views, markers, and decorations. However, users accustomed to the old command-line output may still want to see this raw output as an alternative to other visual forms of feedback. Tools in this category can use the Console view to write this output.

  
Prior to Eclipse 3.0, each plug-in that wanted console-like output created its own Console view. Eclipse 3.0 provides a single generic Console view that all plug-ins can write to. The view can host several console documents at once and allows the user to switch between different console pages. Each page in the console is represented by an org.eclipse.ui.console.IConsole object. To write to the console, you need to create your own IConsole instance and connect it to the Console view. To do this, you have to add a new dependency to org.eclipse.ui.console in the plugin.xml of your plugin. For a console containing a simple text document, you can instantiate a MessageConsole instance. Here is a method that locates a console with a given name and creates a new one if it cannot be found:

      private MessageConsole findConsole(String name) {
         ConsolePlugin plugin = ConsolePlugin.getDefault();
         IConsoleManager conMan = plugin.getConsoleManager();
         IConsole[] existing = conMan.getConsoles();
         for (int i = 0; i < existing.length; i++)
            if (name.equals(existing[i].getName()))
               return (MessageConsole) existing[i];
         //no console found, so create a new one
         MessageConsole myConsole = new MessageConsole(name, null);
         conMan.addConsoles(new IConsole[]{myConsole});
         return myConsole;
      }

  
Once a console is created, you can write to it either by directly modifying its IDocument or by opening an output stream on the console. This snippet opens a stream and writes some text to a console:

      MessageConsole myConsole = findConsole(CONSOLE_NAME);
      MessageConsoleStream out = myConsole.newMessageStream();
      out.println("Hello from Generic console sample action");

  
Creating a console and writing to it do not create or reveal the Console view. If you want to make that sure the Console view is visible, you need to reveal it using the usual workbench API. Even once the Console view is revealed, keep in mind that it may contain several pages, each representing a different IConsole provided by a plug-in. Additional API asks the Console view to display your console. This snippet reveals the Console view and asks it to display a particular console instance:

   IConsole myConsole = ...;// your console instance
   IWorkbenchPage page = ...;// [obtain the active page](./FAQ_How_do_I_find_the_active_workbench_page.md "FAQ How do I find the active workbench page?")
   String id = IConsoleConstants.ID_CONSOLE_VIEW;
   IConsoleView view = (IConsoleView) page.showView(id);
   view.display(myConsole);

  

See Also:
---------

[FAQ How do I use the platform debug tracing facility?](./FAQ_How_do_I_use_the_platform_debug_tracing_facility.md "FAQ How do I use the platform debug tracing facility?")

[FAQ How do I use the text document model?](./FAQ_How_do_I_use_the_text_document_model.md "FAQ How do I use the text document model?")

