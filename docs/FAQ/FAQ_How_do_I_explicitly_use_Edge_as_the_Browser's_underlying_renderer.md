FAQ How do I explicitly use Edge as the Browser's underlying renderer
=====================================================================

To specify that an Edge renderer be used by a Browser instance, create it with style `SWT.EDGE` (since 4.19) or set the Java property `org.eclipse.swt.browser.DefaultType=edge`.

Edge rendering back-end uses the WebView2 component, which is based on, but distinct from the Edge browser itself. WebView2 has to be installed separately from one of the following sources:

* A stand-alone runtime installer, either web or offline ([Download the WebView2 Runtime](https://developer.microsoft.com/en-us/microsoft-edge/webview2/#webview-title) from Microsoft).
  This runtime will be shared between all applications on the machine and will auto-update itself independent of your application.
* A fixed-version archive with all the necessary files (Same link as above).
  This is a complete, fixed set of files to be included with your application. Unlike the first option, you have complete freedom in bundling, packaging and updating it.
* Beta, Dev, or Canary version of the Edge browser (<https://www.microsoftedgeinsider.com/en-us/download>).
  This option is convenient for testing, but production deployments should use the previous two options.

_Note: Stable Edge browser installations don't provide a WebView2 component._

See also [Distribution of apps using WebView2](https://docs.microsoft.com/en-us/microsoft-edge/webview2/concepts/distribution) on MSDN.

SWT will automatically locate installed browsers and runtimes. In case you want to use fixed-version binaries or override the automatically chosen version, set the `org.eclipse.swt.browser.EdgeDir` Java property to the directory containing `msedgewebview2.exe`. For example:

```sh
java "-Dorg.eclipse.swt.browser.EdgeDir=C:\Program Files (x86)\Microsoft\Edge Beta\Application\88.0.705.29" ...
```

WebView2 creates a user data directory to stores caches and persistent data like cookies and local storage. All WebView2 instances in an application and all instances of the same application share this directory.

The default user-directory location is `%LOCALAPPDATA%\<AppName>\WebView2`, where `<AppName>` is defined with `Display.setAppName()`. This location can be overridden on a per-process basis by setting the `org.eclipse.swt.browser.EdgeDataDir` Java property.
