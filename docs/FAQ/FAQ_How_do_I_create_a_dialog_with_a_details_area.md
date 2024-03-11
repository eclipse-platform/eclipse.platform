

FAQ How do I create a dialog with a details area?
=================================================

Many dialogs in Eclipse have a **Details** button that shows or hides an extra area with more information. This functionality is provided by the JFace ErrorDialog. The naming of this class is a bit unfortunate as it doesn't fully express all the things it can be used for. A better name might have been StatusDialog as it is used to display any IStatus object, which can represent information, warnings, or errors. The dialog looks at the severity of the supplied status and uses an appropriate icon: an exclamation mark for errors, a yield sign for warnings, and an _i_ character for information.

  

If you want to provide more information in the details area, you need to supply a MultiStatus object. The dialog will obtain the message string from the MultiStatus parent, and one line in the details area will be for the message from each child status. The following example uses an error dialog to display some information-the date-with more details provided in the details area:

      Date date = new Date();
      SimpleDateFormat format = new SimpleDateFormat();
      String[] patterns = new String[] {
         "EEEE", "yyyy", "MMMM", "h 'o''clock'"};
      String[] prefixes = new String[] {
         "Today is ", "The year is ", "It is ", "It is "};
      String[] msg = new String\[patterns.length\];
      for (int i = 0; i < msg.length; i++) {
         format.applyPattern(patterns[i]);
         msg[i] = prefixes[i] + format.format(date);
      }
      final String PID = ExamplesPlugin.ID;
      MultiStatus info = new MultiStatus(PID, 1, msg[0], null);
      info.add(new Status(IStatus.INFO, PID, 1, msg[1], null));
      info.add(new Status(IStatus.INFO, PID, 1, msg[2], null));
      info.add(new Status(IStatus.INFO, PID, 1, msg[3], null));
      ErrorDialog.openError(window.getShell(), "Time", null, info);

  

  

See Also:
---------

[FAQ\_How\_do\_I\_inform\_the\_user\_of\_a__problem?](./FAQ_How_do_I_inform_the_user_of_a_problem.md "FAQ How do I inform the user of a problem?")

