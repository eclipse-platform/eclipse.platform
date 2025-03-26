# FAQ How do I use Edge/IE as the Browser's underlying renderer?

Since Eclipse/SWT 4.35 (2025-03) Edge is used as the default browser in SWT. For older releases, browser instances using Edge could be created with the style `SWT.EDGE` (since 4.19) or setting the Java property `org.eclipse.swt.browser.DefaultType=edge`. 

## WebView2 Component Provision

Edge rendering backend uses the WebView2 component, which is based on but distinct from the Edge browser itself. SWT relies on a WebView2 component being available on the current system.
There are different [ways of distribution for the WebView2 component](https://learn.microsoft.com/en-us/microsoft-edge/webview2/concepts/distribution). On most systems, the component is already available due to the `Evergreen runtime distribution mode` ensuring the availability of an automatically updated version of the library on Windows.

If this is not the case or if other versions of the component shall be used, these options exist:
* A stand-alone runtime installer, either web or offline ([Download the WebView2 Runtime](https://developer.microsoft.com/en-us/microsoft-edge/webview2/#webview-title) from Microsoft).
  This runtime will be shared between all applications on the machine and will auto-update itself independent of your application.
* A fixed-version archive with all the necessary files (same link as above).
  This is a complete, fixed set of files to be included with your application. Unlike the first option, you have complete freedom in bundling, packaging and updating it.
* Beta, Dev, or Canary version of the Edge browser (<https://www.microsoftedgeinsider.com/en-us/download>).
  This option is convenient for testing, but production deployments should use the previous two options.

SWT will automatically locate installed browsers and runtimes (using the `WebView2Loader.dll` provided via the SWT fragment for Windows).
In case you want to use fixed-version binaries or override the automatically chosen version, set the `org.eclipse.swt.browser.EdgeDir` Java property to the directory containing `msedgewebview2.exe`. For example, you can add `-Dorg.eclipse.swt.browser.EdgeDir=PATH_TO_EDGE` with `PATH_TO_EDGE` being the absolute path to an Edge installation as a command-line argument to an SWT application or to the `eclipse.ini` of an Eclipse product.

## WebView2 Data Directory

WebView2 creates a user data directory to store caches and persistent data like cookies and local storage. All WebView2 instances in an application and all instances of the same application share this directory.

The default user directory is customizable and depends on the usage context:
* In a plain SWT application, the default location is `%LOCALAPPDATA%\<AppName>\EBWebView`, where `<AppName>` is defined with `Display.setAppName()`.
* In an Eclipse product, the default location is inside the metadata folder of the workspace, precisely in `.metadata\.plugins\org.eclipse.swt\EBWebView`.
* The location can be customized by specifying it on a per-process basis via the `org.eclipse.swt.browser.EdgeDataDir` Java property.

## WebView2 Timeouts

All operations on a WebView2 component are executed asynchronously, including it's initialization. Under some conditions and in some environments, operations can take rather long, which is why the WebView2 adaptation in SWT uses timeouts to avoid UI blocks.
In case you run into such timeouts and want to increase the timeout value, you can do so via the `org.eclipse.swt.internal.win32.Edge.timeout` property, which accepts a timeout value in milliseconds.

## Fallback to Internet Explorer

Beginning with 4.35, browser instances in Eclipse/SWT application will use Edge by default and need to be created with style `SWT.IE` or via setting the Java property `org.eclipse.swt.browser.DefaultType=ie` to still use the Internet Explorer.
