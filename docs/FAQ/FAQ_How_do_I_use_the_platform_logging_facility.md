

FAQ How do I use the platform logging facility?
===============================================

The Eclipse runtime plug-in provides a simple set of APIs for logging exceptions, warnings, or other information useful in debugging or servicing a deployed Eclipse product. The intent of the log is to record information that can be used later to diagnose problems in the field. Because this information is not directed at users, you do not need to worry about translating messages or simplifying explanations into a form that users will understand. The idea is that when things go wrong in the field, your users can send you the log file to help you figure out what happened.

Each plug-in has its own log associated with it, but all logged information eventually makes its way into the platform log file (see the getLogFileLocation method on Platform).

You can write any kind of IStatus object to the log file, including a MultiStatus if you have hierarchies of information to display. If you create your own subclass of the utility class Status, you can override the getMessage method to return extra information to be displayed in the log file. Many plug-ins add utility classes for writing messages and errors to the log:

      import org.eclipse.core.runtime.Status;
      ...
      public class MyLogger {
      ...
      private static final Bundle BUNDLE = FrameworkUtil.getBundle(MyLogger.class);
      private static final ILog LOGGER = Platform.getLog(BUNDLE);
      ...
      public static void test(){
         try {
            log("Eclipse Style Logging");
         }
         catch(Exception e){
            log("Oops!", e);
         }
      }

      public static void log(String msg) {
         log(msg, null);
      }

      public static void log(String msg, Exception e) {
         LOGGER.log(new Status((e==null?Status.INFO:Status.ERROR), BUNDLE.getSymbolicName(), msg, e));
      }

During development, you can browse and manipulate the platform log file using the Error Log view (**Window > Show View > General > Error Log**). You can also have the log file mirrored in the Java console by starting Eclipse with the -consoleLog command-line argument.

      eclipse -vm c:\\jre\\bin\\java.exe -consoleLog

We explicitly pass the VM because on Windows you have to use java.exe instead of javaw.exe if you want the Java console window to appear.

See Also:
---------

*   [FAQ Where can I find that elusive .log file?](./FAQ_Where_can_I_find_that_elusive_log_file.md "FAQ Where can I find that elusive .log file?")
*   [Platform UI Error Handling](/Platform_UI_Error_Handling "Platform UI Error Handling")

