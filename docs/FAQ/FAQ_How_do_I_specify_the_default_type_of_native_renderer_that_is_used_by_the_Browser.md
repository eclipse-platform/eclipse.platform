FAQ How do I specify the default type of native renderer that is used by the Browser
====================================================================================

The default native renderers that are used for `SWT.NONE`-style Browsers are listed in [Which platforms support the SWT Browser, and which native renderers do they use?](./FAQ_Which_platforms_support_the_SWT_Browser,_and_which_native_renderers_are_available.md). Default is chosen to not require additional software installation and to preserve backward-compatible behavior.

A user can set a property to specify the type of native renderer to use for `SWT.NONE`-style Browsers. Setting this property does not affect Browsers that are created with explicit renderer styles such as `SWT.WEBKIT` or `SWT.CHROMIUM`. The property name is `org.eclipse.swt.browser.DefaultType` and valid values for it currently include `webkit`, `ie` (since 4.3), `chromium` (since 4.17) and `edge` (since 4.19). This property must be set before the first `Browser` instance is created.

_Note: As of Eclipse/SWT 4.8, Mozilla (`XULRunner`) renderer is no longer supported, the value `mozilla` has no effect._

A user can specify a comma-separated list of native renderers, in order of preference, for the `org.eclipse.swt.browser.DefaultType` value. Values not applicable to a particular platform are ignored. For example, the value of `edge,chromium` will change the default to Edge on Windows and Chromium on other platforms.

The best opportunity for a user to set this property is by launching their application with a `-D` VM switch (e.g., add to the end of the `eclipse.ini` file: `-Dorg.eclipse.swt.browser.DefaultType=chromium`).

An alternate approach that an Eclipse application may use is to provide a `BrowserInitializer` implementation that sets this property. This implementation will be invoked when the first Browser instance is about to be created. The steps to do so are:

* Create a fragment with host plug-in `org.eclipse.swt`.
* In this fragment, create class `org.eclipse.swt.browser.BrowserInitializer`.
* Implement a static initializer in this class that sets the `org.eclipse.swt.browser.DefaultType` property.
