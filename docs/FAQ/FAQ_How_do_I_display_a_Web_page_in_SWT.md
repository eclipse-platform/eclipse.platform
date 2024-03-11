FAQ How do I display a Web page in SWT?
=======================================

In Eclipse 3.0, SWT introduced a browser widget for displaying a native HTML renderer inside an SWT control. Prior to the introduction of this browser, it was necessary to invoke an external Web browser program for displaying rendered HTML. The browser can be instructed to render either a URL or a supplied string containing HTML content. The browser widget does not include the usual controls for navigation, bookmarks, and all the usual bells and whistles associated with a Web browser. As such, it can be used for highly controlled applications, such as displaying help text or even for showing decorated and interactive text inside a view or an editor.

The browser has API for programmatically manipulating the content, such as browsing forward or back in the navigation history, refreshing the content, or halting a rendering in process. You can install listeners on the browser to be notified when the location is changing or when the title changes or to receive progress notification as a page loads. It is fairly straightforward to implement basic Web browser functionality around this browser widget. For more details, take a look at BrowserAction in the org.eclipse.faq.examples plug-in. This action implements a fully functional Web browser in fewer than 60 lines of code!

As a quick example, here is a stand-alone SWT snippet that opens a browser shell on this book's Web site.

A title listener is added to the browser in order to update the shell title with the name of the Web page being displayed:

      Display display = new Display();
      final Shell shell = new Shell(display, SWT.SHELL_TRIM);
      shell.setLayout(new FillLayout());
      Browser browser = new Browser(shell, SWT.NONE);
      browser.addTitleListener(new TitleListener() {
         public void changed(TitleEvent event) {
            shell.setText(event.title);
         }
      });
      browser.setBounds(0,0,600,400);
      shell.pack();
      shell.open();
      browser.setUrl("https://eclipse.org");
      while (!shell.isDisposed())
         if (!display.readAndDispatch())
            display.sleep();

  
Figure 7.1 shows the resulting browser inside a simple shell. The browser widget is not yet available on all platforms as not all platforms that SWT supports have an appropriate native control that can be exploited. For Eclipse 3.0, the browser will at least be available on Windows, Linux, QNX, and MacOS. For platforms that do not have a browser widget available, the Browser constructor will throw an SWT error, allowing you to catch the condition and fall back to an alternative, such as a user-specified external browser.

How can I invoke the eclipse default web browser in my own plugin?
------------------------------------------------------------------

      > > I want to invoke the eclipse default web browser which is specified in 
      > > "Preferences" -> "General" -> "Web Browser" in my own plugin, giving it 
      > > a URL and make it visit that URL. How can I do this? Thanks.

I think you want something like...

      IWorkbenchBrowserSupport support =
      PlatformUI.getWorkbench().getBrowserSupport();
      IWebBrowser browser = support.createBrowser("someId");
      browser.openURL(new URL("https://www.eclipse.org"));


