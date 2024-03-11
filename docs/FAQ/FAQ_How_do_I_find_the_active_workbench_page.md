

FAQ How do I find the active workbench page?
============================================

Many workbench APIs are accessible only from IWorkbenchWindow or IWorkbenchPage. This generally raises the question, How do I get a reference to a window or a page?

As it turns out, the answer isn't always straightforward. There appears to be an obvious API on IWorkbench for getting this (caution: _please read below before using this code!_):

 

       IWorkbench wb = PlatformUI.getWorkbench();
       IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
       IWorkbenchPage page = win.getActiveWorkbenchPage();
     
       // on new versions it may need to be changed to:
       IWorkbenchPage page = win.getActivePage();

However, if you read the fine print on these methods, you'll see that they can — and do! — return null if the active shell is not a window. This means that when a dialog or other shell has focus, you might not be able to use these APIs to access the active window or page.

Moreover the active window may not be the _appropriate_ window. On some window systems, the user can click on a toolbar item in an inactive window.

To avoid getting null windows and pages, you should get your window or a page in another way.

Contents
--------

*   [1 Within a Workbench Part](#Within-a-Workbench-Part)
*   [2 Within a Command Handler](#Within-a-Command-Handler)
*   [3 Within an Action](#Within-an-Action)
*   [4 From an IEclipseContext or IServiceLocator](#From-an-IEclipseContext-or-IServiceLocator)

Within a Workbench Part
-----------------------

From within the implementation of any view or editor, you can do the following:

 

       IWorkbenchPage page = getSite().getPage();

Workbench sites are IServiceLocator.

Within a Command Handler
------------------------

The HandlerUtil class provides a number of helper methods to obtain the active window, editor, part, etc. from the ExecutionEvent provided to the handler.

A special case is a handler that implements IElementUpdater as the updateElements() method is provided an UIElement rather than an ExecutionEvent. But the UIElement does provide an IServiceLocator.

Within an Action
----------------

From an action defined in a workbench action set, you can access the window from the init method:

 

       class MyAction implements IWorkbenchWindowActionDelegate {
          private IWorkbenchWindow window;
          ...
          public void init(IWorkbenchWindow win) {
             this.window = win;
          }
       }

Similarly, actions contributed to the popupMenus extension point always have an initialization method that sets the current part before the action's run method is called. All wizard extension points also have an IWorkbenchWizard init method that supplies the wizard with the current workbench window before the wizard is launched. In short, if you look carefully, you can almost always get at the current window or page, no matter where you are in the Eclipse UI.

From an IEclipseContext or IServiceLocator
------------------------------------------

Service locators are similar to Eclipse Contexts (IEclipseContext):

 

       IServiceLocator locator = …;
       IWorkbenchWindow window = locator.get(IWorkbenchWindow.class);

