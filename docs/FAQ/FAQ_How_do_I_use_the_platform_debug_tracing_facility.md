

FAQ How do I use the platform debug tracing facility
====================================================

Adding tracing to your code
---------------------------

During development, it is common practice to print debugging messages to standard output. One common idiom for doing this is

 

       private static final boolean DEBUG = true;
       ...
       if (DEBUG)
          System.out.println("So far so good");

The advantage of this approach is that you can flip the DEBUG field to false when it comes time to deploy your code. The Java compiler will then remove the entire if block from the class file as flow analysis reveals that it is unreachable. The downside of this approach is that all the hard work that went into writing useful debug statements is lost in the deployed product. If your user calls up with a problem, you will have to send a new version of your libraries with the debug switches turned on before you can get useful feedback. Eclipse provides a tracing facility that is turned off by default but can be turned on in the field with a few simple steps.

To instrument your code, use Platform.getDebugOption to add conditions to your trace statements:

 

       private static final String DEBUG_ONE = 
          "org.eclipse.faq.examples/debug/option1";
       ...
       String debugOption = Platform.getDebugOption(DEBUG_ONE);
       if ("true".equalsIgnoreCase(debugOption))
          System.out.println("Debug statement one.");

If you do not need dynamic trace enablement or if you are concerned about code clutter or performance, another tracing style yields cleaner and faster source:

 

       private static final boolean DEBUG_TWO = "true".equalsIgnoreCase(Platform.getDebugOption(
             "org.eclipse.faq.examples/debug/option2"));
       ...
       if (DEBUG_TWO)
          System.out.println("Debug statement two.");

  
This tracing style is not quite as good as the standard approach outlined at the beginning of this FAQ. Because the debug flag cannot be computed statically, the compiler will not be able to completely optimize out the tracing code. You will still be left with the extra code bulk, but the performance will be good enough for all but the most extreme applications.

**Note:** Some projects use Platform.getDebugOption("pluginID/debug") as their master switch.

Turning on debug tracing
------------------------

To turn tracing on, you need to create a trace-options file that contains a list of the debug options that you want to turn on. By default, the platform looks for a file called .options in the Eclipse install directory. This should be a text file in the Java properties file format, with one key=value pair per line. To turn on the trace options in the preceding two examples, you need an options file that looks like this:

      org.eclipse.faq.examples/debug=true
      org.eclipse.faq.examples/debug/option1=true
      org.eclipse.faq.examples/debug/option2=true

The first line sets the value of the flag returned by Plugin.isDebugging, and the next two lines define the debug option strings returned by the getDebugOption method on Platform.

_Hint_: If you use tracing in your plug-in, you should keep in your plug-in install directory a .options file that contains a listing of all the possible trace options for your plug-in. This advertises your tracing facilities to prospective users and developers; in addition, the Run-time Workbench launch configuration will detect this file and use it to populate the Tracing Options page (Figure 6.1). If you browse through the plug-in directories of the Eclipse SDK, you will see that several plug-ins use this technique to document their trace options; for example, see org.eclipse.core.resources or org.eclipse.jdt.core.

_Hint2_: If the trace levels don't show up in the according dialogs (Run/Debug configuration or in the workspace settings of the eclipse product) you may have forgotten to include the .options file in the Build Configuration (build.properties) of your plug-in.


Finally, you need to enable the tracing mechanism by starting Eclipse with the -debug command line argument. You can, optionally, specify the location of the debug options file as either a URL or a file-system path after the -debug argument.

