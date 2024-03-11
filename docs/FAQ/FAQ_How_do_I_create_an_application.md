

FAQ How do I create an application?
===================================

To create an application, you need a plug-in that adds an extension to the org.eclipse.core.runtime.applications extension point. An example application definition from a plugin.xml file is as follows:

      <extension id="helloworld" point="org.eclipse.core.runtime.applications">
         <application>
            <run class="org.eclipse.faq.HelloWorld"/>
         </application>
      </extension>

  
The class attribute of the run element must specify a class that implements org.eclipse.core.boot.IPlatformRunnable. Here is the source of a trivial application:

      public class HelloWorld implements IPlatformRunnable {
         public Object run(Object args) throws Exception {
            System.out.println("Hello from Eclipse application");
            return EXIT_OK;
         }
      }

To run the application, you need to specify the fully qualified ID of your application extension definition, using the application command-line argument when launching Eclipse:

      eclipse -application org.eclipse.faq.helloworld.helloworld

The fully qualified extension ID is computed by prepending the plug-in ID to the simple extension ID from the plugin.xml file. In this example, the plug-in ID is org.eclipse.faq.helloworld, and the simple extension ID is helloworld.

