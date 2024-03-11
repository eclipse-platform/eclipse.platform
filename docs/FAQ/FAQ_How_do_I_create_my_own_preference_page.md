

FAQ How do I create my own preference page?
===========================================

JFace provides infrastructure for creating and presenting preference pages within a Preference dialog. Pages implement IPreferencePage, usually by subclassing the default implementation PreferencePage or FieldEditorPreferencePage. The org.eclipse.ui.workbench plug-in defines an extension point for contributing preference pages to the workbench preference dialog, typically available via **Window > Preferences**. Note that you can use the preference page infrastructure without even using the preferences extension point if you want to present preference pages outside the standard workbench Preference dialog.

See Also:
---------

*   [FAQ How do I launch the preference page that belongs to my plug-in?](./FAQ_How_do_I_launch_the_preference_page_that_belongs_to_my_plug-in.md "FAQ How do I launch the preference page that belongs to my plug-in?")
*   [Eclipse online article Preferences in the Eclipse Workbench UI (Revised for 2.0)](https://www.eclipse.org/articles/Article-Preferences/preferences.htm)
*   [Eclipse online article Mutatis Mutandis-Using Preference Pages as Property Pages](https://www.eclipse.org/articles/Article-Mutatis-mutandis/overlay-pages.html)
*   [Simplifying Preference Pages with Field Editors](https://www.eclipse.org/articles/Article-Field-Editors/field_editors.html)
*   [Implementing a preference page (Platform Plug-in Developer's Guide)](https://help.eclipse.org/help31/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/preferences_prefs_implement.htm)

