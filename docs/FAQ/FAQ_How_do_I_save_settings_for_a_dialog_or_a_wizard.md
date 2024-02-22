

FAQ How do I save settings for a dialog or wizard?
==================================================

The IDialogSettings mechanism can be used to store any hierarchical collection of strings and numbers. It is used in Eclipse to persist preferences and history for most views, dialogs, and wizards. For example, dialogs will often store the last few values entered in each entry field and allow the user to select from these previous values, using a combo box. Views will use dialog settings to store filtering, sorting, and layout preferences. In all these cases, the basic steps involved are the same.

First, you need to find or create an instance of IDialogSettings. If your plug-in is a subclass of AbstractUIPlugin, your plug-in already has a global setting object. Simply call the method getDialogSettings on your plug-in to obtain an instance. If you're not using AbstractUIPlugin, you can create an instance of DialogSettings yourself.

Once you have a settings instance, you simply store the data values you want to keep on the settings object. If you're using a global settings object, you can add subsections, using addNewSection. Typically, each dialog, wizard page, and view will have its own settings section within the global settings instance.

Finally, the settings must be saved to disk at some point. If you're using the AbstractUIPlugin settings instance, it is saved automatically when the plug-in is shut down. If you created the settings yourself, you have to save the settings manually when you're finished changing them by calling the save method. Note that each plug-in has a state location allotted to it, and you can find out the location by calling getStateLocation on your plug-in instance. The following example loads a settings file from disk, changes some values, and then saves it again:

      IPath path = ExamplesPlugin.getDefault().getStateLocation();
      String filename = path.append("settings.txt").toOSString();
      DialogSettings settings = new DialogSettings("Top");
      settings.load(filename);
      settings.put("Name", "Joe");
      settings.put("Age", 35);
      settings.save(filename);

See Also:
---------

*   [FAQ How does a view persist its state between sessions?](./FAQ_How_does_a_view_persist_its_state_between_sessions.md "FAQ How does a view persist its state between sessions?")

