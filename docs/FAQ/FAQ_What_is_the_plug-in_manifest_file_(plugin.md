

FAQ What is the plug-in manifest file (plugin.xml)?
===================================================

The plug-in manifest file, plugin.xml, describes how the plug-in extends the platform, what extensions it publishes itself, and how it implements its functionality. The manifest file is written in XML and is parsed by the platform when the plug-in is loaded into the platform. All the information needed to display the plug-in in the UI, such as icons, menu items, and so on, is contained in the manifest file. The implementation code, found in a separate Java JAR file, is loaded when, and only when, the plug-in has to be run. This concept is referred to as _lazy loading_. Here is the manifest file for a simple plug-in:

      <?xml version="1.0" encoding="UTF-8"?>
      <?eclipse version="3.0"?>
      <plugin id="com.xyz.myplugin" name="My Plugin" class="com.xyz.MyPlugin" version="1.0">
         <runtime>
            <library name="MyPlugin.jar"/>
         </runtime>
         <requires>
            <import plugin="org.eclipse.core.runtime"/>
         </requires>
      </plugin>

The processing instructions at the beginning specify the XML version and character encoding and that this plug-in was built for version 3.0 of the Eclipse Platform. The plugin element specifies the basic information about the plug-in, including, in this case, the optional class attribute to specify an instance of the Plugin class associated with this plug-in. Because it contains a subclass of Plugin, this plug-in must include a runtime element that specifies the JAR file that contains the code and a requires element to import the org.eclipse.core.runtime plug-in where the superclass resides. The manifest may also specify _extensions_ and _extension points_ associated with the plug-in. Of all this, only the plugin element with the id, name, and version attributes are required.

  
It is possible to internationalize the strings within a plugin by moving some of the xml attribute values into .properties files. The PDE will assist you in this as follows:

1.  Right click on the plugin.xml or MANIFEST.MF in the project view.
2.  Pick "PDE Tools->Externalize Strings...".
3.  Decide which strings should be moved and what their property names will be.

Helpful property names can make a translator's job much easier.

"command.name.3"? Not so much.

These externalized strings will appear in your plugin.xml as, for example:

      <view
      category="fun.with.typesetting"
      name=**"%typesetter.view.name"**
      class="...
      />

This would be paired with an entry in a .properties file (as of 3.6, Eclipse creates a file called "/myProject/OSGI-INF/l10n/bundle.properties") thusly:

      typesetter.view.name=Etaoin Shrdlu

This properties file is also added to the list of _bin.includes_ in your plugin's _build.properties_ file.

A properly internationalized plugin can then be localized using [plugin fragments](./FAQ_What_is_a_plug-in_fragment.md "FAQ What is a plug-in fragment?").

See Also:
---------

*   [FAQ What is a plug-in?](./FAQ_What_is_a_plug-in.md "FAQ What is a plug-in?")
*   [FAQ What are extensions and extension points?](./FAQ_What_are_extensions_and_extension_points.md "FAQ What are extensions and extension points?")
*   [FAQ When does a plug-in get started?](./FAQ_When_does_a_plug-in_get_started.md "FAQ When does a plug-in get started?")
*   [FAQ What is a plug-in fragment?](./FAQ_What_is_a_plug-in_fragment.md "FAQ What is a plug-in fragment?")

