

FAQ How do I launch the preference page that belongs to my plug-in?
===================================================================

Sometimes, you want to allow the user to edit preferences for a plug-in quickly, without manually opening the Preference dialog via **Window > Preferences** and locating the page for the plug-in.

The `org.eclipse.ui.dialogs.PreferencesUtil` in bundle `org.eclipse.ui.workbench` can be used for this:

        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
            shell, "org.eclipse.licensing.ui.licensesPreferencePage",  
            new String[] {"org.eclipse.licensing.ui.licensesPreferencePage"}, null);
        dialog.open();

Alternative solution
--------------------

_This solution is old and probably not useful anymore (Please note, that the constructor "PreferenceDialog()" is not API anymore). Maybe it should be removed from this wiki page, but the author of this edit is not sure whether there really is no cases where it is useful._

This solution works by launching the Preference dialog yourself with a customized PreferenceManager, a class responsible for building the tree of preference nodes available on the left-hand side of the Preference dialog.

For each preference page you want to display, create a PreferenceNode instance that contains a reference to your page and some unique page ID. Pages are then added to a preference manager. If you want to create a hierarchy of pages, use the PreferenceManager method addTo, where the path is a period-delimited series of page IDs above the page being added. Finally, create an instance of PreferenceDialog, passing in the preference manager you have created. Here is sample code for opening the Preference dialog on a single preference page called MyPreferencePage:

            IPreferencePage page = new MyPreferencePage();
            PreferenceManager mgr = new PreferenceManager();
            IPreferenceNode node = new PreferenceNode("1", page);
            mgr.addToRoot(node);
            PreferenceDialog dialog = new PreferenceDialog(shell, mgr);
            dialog.create();
            dialog.setMessage(page.getTitle());
            dialog.open();

See Also:
---------

*   [FAQ How do I create my own preference page?](./FAQ_How_do_I_create_my_own_preference_page.md "FAQ How do I create my own preference page?")

