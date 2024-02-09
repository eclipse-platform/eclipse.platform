

FAQ How do I load and save plug-in preferences?
===============================================

Current Practice
----------------

As of 3.1 the technique to modify preference values goes something like this:

  private void savePluginSettings() {
    // saves plugin preferences at the workspace level
    Preferences prefs =
      InstanceScope.INSTANCE.getNode(MY\_PLUGIN\_ID); // does all the above behind the scenes

    prefs.put(KEY1, this.someStr);
    prefs.put(KEY2, this.someBool);

    try {
      // prefs are automatically flushed during a plugin's "super.stop()".
      prefs.flush();
    } catch(BackingStoreException e) {
      //TODO write a real exception handler.
      e.printStackTrace();
    }
  }

  private void loadPluginSettings() {
    Preferences prefs = new InstanceScope().getNode(MY\_PLUGIN\_ID);
    // you might want to call prefs.sync() if you're worried about others changing your settings
    this.someStr = prefs.get(KEY1);
    this.someBool= prefs.getBoolean(KEY2);
  }

See Also:
---------

*   [FAQ How do I use the preference service?](./FAQ_How_do_I_use_the_preference_service.md "FAQ How do I use the preference service?")
*   [FAQ What is a preference scope?](./FAQ_What_is_a_preference_scope.md "FAQ What is a preference scope?")

