

FAQ How do I specify where application data is stored?
======================================================

  

  

  

Plug-in metadata, as well as other data associated with an Eclipse application, is usually stored inside the _platform instance location_. This location is also known as the _instance data area_ or the _workspace_. There are a number of ways that your application can specify this location:

*   Pick a fixed location and specify it using the -data command-line

argument on startup (for example, from your own native launcher or launch script).</li>

*   Pick a location and specify it programmatically when your application starts.</li>
*   Let the default location be used. By default, the data area will be under

the current working directory on startup in a subdirectory called workspace. This typically corresponds to the eclipse base directory. </li>

*   Prompt the user to specify a location on startup. This should be done

from your application's run method before you call PlatformUI.createAndRunWorkbench to open the workbench.

If you define a location programmatically (either by prompting the user or by other means), you must then set it as follows:

      URL choice = ... pick a data location
      Location loc = Platform.getInstanceLocation();
      if (loc.setURL(choice, true))
         //success!
      else
         //location is in use, or is invalid

If your end-user is allowed to manipulate the command line directly, there are other things you need to keep in mind. For example, the user may have already picked a location using -data. In this case, Location.isSet() will return true, but you are still responsible for locking the location using Location.lock() to prevent other instances of your application from trying to use the same location concurrently. To see all of the cases that need to be considered take a look at how the Eclipse IDE application does it. Look at IDEApplication.checkInstanceLocation to see all the subtleties of checking and prompting for an instance location.

See Also:
---------

[FAQ\_How\_do\_I\_run_Eclipse?](./FAQ_How_do_I_run_Eclipse.md "FAQ How do I run Eclipse?")

[FAQ\_Where\_do\_plug-ins\_store\_their\_state?](./FAQ_Where_do_plug-ins_store_their_state.md "FAQ Where do plug-ins store their state?")

\[\[FAQ\_Can\_I\_create\_an\_application\_that\_doesn%26%23146%3Bt\_have\_a\_data_location%3F\]\]

