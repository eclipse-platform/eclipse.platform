

FAQ Where do plug-ins store their state?
========================================

Plug-ins store data in two standard locations. First, each plug-in has its own install directory that can contain any number of files and folders. The install directory must be treated as read-only, as a multi-user installation of Eclipse will typically use a single install location to serve many users. However, your plug-in can still store read-only information there, such as images, templates, default settings, and documentation.

The second place to store data is the plug-in state location. Each plug-in has within the user's workspace directory a dedicated subdirectory for storing arbitrary settings and data files. This location is obtained by calling the method getStateLocation on your Plugin instance. Generally, this location should be used only for cached information that can be recomputed when discarded, such as histories and search indexes. Although the platform will never delete files in the plug-in state location, users will often export their projects and preferences into a different workspace and expect to be able to continue working with them. If you are storing information that the user may want to keep or share, you should either store it in a location of the user's choosing or put it in the preference store. If you allow the user to choose the location of data, you can always store the location information in a file in the plug-in state location.

Plug-ins can store data that may be shared among several workspaces in two locations. The _configuration location_ is the same for all workspaces launched on a particular configuration of Eclipse plug-ins. You can access the root of this location by using getConfigurationLocation on Platform. The _user location_ is shared by all workspaces launched by a particular user and is accessed by using getUserLocation on Platform.

Here is an example of obtaining a lock on the user location:

      Location user = Platform.getUserLocation();
      if (user.lock()) {
         // read and write files
      } else {
         // wait until lock is available or fail
      }

Note that these locations are accessible to all plug-ins, so make sure that any data stored here is in a unique subdirectory based on your plug-in's unique ID. Even then, keep in mind that a single user may open multiple workspaces simultaneously that have access to these areas. If you are writing files in these shared locations, you must make sure that you protect read-and-write access by locking the location.

See Also:
---------

*   [FAQ How do I find out the install location of a plug-in?](./FAQ_How_do_I_find_out_the_install_location_of_a_plug-in.md "FAQ How do I find out the install location of a plug-in?")
*   [FAQ What is a configuration?](./FAQ_What_is_a_configuration.md "FAQ What is a configuration?")
*   [FAQ How do I load and save plug-in preferences?](./FAQ_How_do_I_load_and_save_plug-in_preferences.md "FAQ How do I load and save plug-in preferences?")

