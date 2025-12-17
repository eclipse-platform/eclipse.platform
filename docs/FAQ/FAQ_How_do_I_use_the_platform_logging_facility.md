FAQ How do I use the platform logging facility?
===============================================

The Eclipse ILog class provides a simple set of APIs for logging exceptions, warnings, or other information useful in debugging or servicing a deployed Eclipse product.
The intent of the log is to record information that can be used later to diagnose problems in the field.
Because this information is not directed at users, you do not need to worry about translating messages or simplifying explanations into a form that users will understand.
The idea is that when things go wrong in the field, your users can send you the log file to help you figure out what happened.

Each plug-in has its own log associated with it, but all logged information eventually makes its way into the platform log file (see the getLogFileLocation method on Platform).

```java
      import org.eclipse.core.runtime.ILog;
      ...
      public class MyLogger {
      ...
      private static final Bundle BUNDLE = FrameworkUtil.getBundle(MyLogger.class);


      ...
      public static void test(){
         ILog.get().info("Starting");
         try {
            // do something that may cause an exception
         }
         catch (Exception e){
          ILog.get().error("Generating template failed!", e);
          }
         ILog.get().warn("Creating a warning..");
      }
```

During development, you can browse and manipulate the platform log file using the Error Log view (**Window > Show View > General > Error Log**).
You can also have the log file mirrored in the Java console by starting Eclipse with the -consoleLog command-line argument.

      eclipse -vm c:\\jre\\bin\\java.exe -consoleLog

We explicitly pass the VM because on Windows you have to use java.exe instead of javaw.exe if you want the Java console window to appear.

You can also write any kind of IStatus object to the log file, including a MultiStatus if you have hierarchies of information to display.
If you create your own subclass of the utility class Status, you can override the getMessage method to return extra information to be displayed in the log file.



See Also:
---------

*   [FAQ Where can I find that elusive .log file?](./FAQ_Where_can_I_find_that_elusive_log_file.md "FAQ Where can I find that elusive .log file?")
*   [Platform UI Error Handling](/Platform_UI_Error_Handling "Platform UI Error Handling")

