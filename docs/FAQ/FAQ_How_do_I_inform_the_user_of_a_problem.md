

FAQ How do I inform the user of a problem?
==========================================

Warnings and errors can be reported to the user by using an ErrorDialog. Here is a very simple example of an Error dialog displaying a warning message:

      IStatus warning = new Status(IStatus.WARNING, 
         ExamplesPlugin.ID, 1, "You have been warned.", null);
      ErrorDialog.openError(window.getShell(), 
         "This is your final warning", null, warning);

  

The null parameter to the status constructor is for supplying an exception object. The exception object is not used by the Error dialog, so we'll leave it blank in this case. If you're creating a status object for another purpose, such as logging, you'll want to supply the exception, if there is one. The openError method has parameters for a title and a message, but both of these parameters are optional. If you don't supply a title, the method will use the generic **Problems Occurred** message. If you don't supply a message, the method will take the message from the supplied status object.

  

  

  

See Also:
---------

[Platform UI Error Handling](/Platform_UI_Error_Handling "Platform UI Error Handling")

[FAQ\_How\_do\_I\_use\_the\_platform\_logging\_facility?](./FAQ_How_do_I_use_the_platform_logging_facility.md "FAQ How do I use the platform logging facility?")

[FAQ\_How\_do\_I\_create\_a\_dialog\_with\_a\_details\_area?](./FAQ_How_do_I_create_a_dialog_with_a_details_area.md "FAQ How do I create a dialog with a details area?")

