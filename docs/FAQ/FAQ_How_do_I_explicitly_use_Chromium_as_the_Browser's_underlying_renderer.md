FAQ How do I explicitly use Chromium as the Browser's underlying renderer
=========================================================================

To specify that a Chromium renderer be used by a Browser instance, create it with style `SWT.CHROMIUM` (since 4.17) or set the Java property `org.eclipse.swt.browser.DefaultType=chromium`.

You can get the SWT-Chromium libraries from the Eclipse SDK or from the standalone SWT Chromium support libraries section on the download page.

To use the Chromium libraries from the Eclipse SDK:

* Install the CEF binaries in Eclipse from the p2 repo - [CEF p2 repo from Make technology](https://dl.equo.dev/chromium-swt-ce/oss/p2)
* Add the required jars to classpath of project:
  * SWT-Chromium fragment (`org.eclipse.swt.browser.chromium.<ws>.<os>.<arch>.jar`)
  * SWT fragment (`org.eclipse.swt.<ws>.<os>.<arch>.jar`)
  * CEF binary (`com.make.chromium.cef.<ws>.<os>.<arch>.jar`)

To use the Chromium libraries from the standalone SWT downloads:

* Get CEF binaries for your platform from the p2 repo:
  * [CEF GTK binaries](https://dl.equo.dev/chromium-swt-ce/oss/mvn/com/equo/com.equo.chromium.cef.gtk.linux.x86_64/128.0.0/com.equo.chromium.cef.gtk.linux.x86_64-128.0.0.jar)
  * [CEF Mac binaries](https://dl.equo.dev/chromium-swt-ce/oss/mvn/com/equo/com.equo.chromium.cef.cocoa.macosx.x86_64/128.0.0/com.equo.chromium.cef.cocoa.macosx.x86_64-128.0.0.jar)
  * [CEF Windows binaries](https://dl.equo.dev/chromium-swt-ce/oss/mvn/com/equo/com.equo.chromium.cef.win32.win32.x86_64/128.0.0/com.equo.chromium.cef.win32.win32.x86_64-128.0.0.jar)
* Add the required jars to classpath of project:
  * SWT-Chromium standalone jar (`swt-chromium.jar`)
  * SWT standalone jar (`swt.jar`)
  * CEF binary (`com.make.chromium.cef.<ws>.<os>.<arch>.jar`)

To launch Eclipse with Chromium as the default browser type:

* Install the CEF binaries in Eclipse from the p2 repo - [CEF p2 repo from Make technology](https://dl.equo.dev/chromium-swt-ce/oss/p2)
* In `eclipse.ini`, add `-Dorg.eclipse.swt.browser.DefaultType=chromium` under `-vmargs`.
