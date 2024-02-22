

FAQ What is a preference scope?
===============================

The preference service uses the notion of _preference scopes_ to describe the various areas where preferences can be saved. The platform includes the following preference scopes:

*   _Configuration scope_. Preferences stored in this scope are shared by all workspaces that are launched using a particular configuration of Eclipse plug-ins. On a single-user installation, this serves to capture preferences that are common to all workspaces launched by that user. On a multi-user installation, these preferences are shared by all users of the configuration.

*   _Instance scope_. Preferences in this scope are specific to a single Eclipse workspace. The old API method getPluginPreferences on Plugin stores its preferences at this scope.

*   _Default scope_. This scope is not stored on disk at all but can be used to store default values for all your keys. When values are not found in other scopes, the default scope is consulted last to provide reasonable default values.

*   _BundleDefaultsScope_. Similar to the default scope, these values are not written to disk. They are however read from a particular bundle's "preferences.ini" file.

*   _Project scope_. This scope stores values that are specific to a single project in your workspace, such as code formatter and compiler settings. Note that this scope is provided by the org.eclipse.core.resources plug-in, which is not included in the Eclipse Rich Client Platform. This scope will not exist in applications that don't explicitly include the resources plug-in.

Plug-ins can also define their own preference scopes, using the org.eclipse.core.runtime.preferences extension point. If you define your own scope, you can control how and where your preferences are loaded and stored. However, for most clients, the built in scopes will be sufficient.

The majority of the above scopes can be addressed programatically with a particular implementation of [IScopeContext](https://help.eclipse.org/latest/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/api/org/eclipse/core/runtime/preferences/IScopeContext.html):

*   [ConfigurationScope](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/core/runtime/preferences/ConfigurationScope.html)
*   [InstanceScope](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/core/runtime/preferences/InstanceScope.html)
*   [DefaultScope](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/core/runtime/preferences/DefaultScope.html)
*   [ProjectScope](https://help.eclipse.org/latest/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/api/org/eclipse/core/resources/ProjectScope.html)

See Also:
---------

*   [FAQ\_What\_is\_a\_configuration?](./FAQ_What_is_a_configuration.md "FAQ What is a configuration?")
*   [FAQ\_How\_do\_I\_use\_the\_preference_service?](./FAQ_How_do_I_use_the_preference_service.md "FAQ How do I use the preference service?")

