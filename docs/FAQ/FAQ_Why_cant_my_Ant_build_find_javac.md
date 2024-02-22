

FAQ Why can't my Ant build find javac?
======================================

Ant tasks that include the javac task for compiling Java source will fail if a Java compiler cannot be found. This results in an error message like the following:

   Unable to find a javac compiler;
   com.sun.tools.javac.Main is not on the classpath.
   Perhaps JAVA_HOME does not point to the JDK

This simply means that Ant could not find a Java compiler. The easiest solution is to make sure that tools.jar, which is included with any JDK-as opposed to a JRE-is on Ant's classpath. You can add items to Ant's classpath from the **Ant > Runtime** preference page. If you launch Eclipse by using a full JDK instead of a JRE, tools.jar should appear on the Ant classpath automatically.

  
Alternatively, Ant supports the notion of a _compiler adapter_, allowing you to plug in your own Java compiler, such as the Java compiler that is built into Eclipse. The Eclipse compiler adapter is found in the org.eclipse.jdt.core in jdtCompilerAdapter.jar. Again, you need to make sure that this JAR is on Ant's classpath from the Ant preference page. Then, simply add the following line to your build file to specify the compiler:

      <property name="build.compiler" 
         value="org.eclipse.jdt.core.JDTCompilerAdapter"/>
 

  

See Also:
---------

[FAQ What is Ant?](./FAQ_What_is_Ant.md "FAQ What is Ant?")

