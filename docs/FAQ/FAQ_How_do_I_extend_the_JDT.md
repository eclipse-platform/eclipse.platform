

FAQ How do I extend the JDT?
============================

  

The first step is to read **Help > Help Contents... > JDT Plug-in Developer Guide**. This guide provides an excellent description of how the Java development tools maintain a model of a Java program and what can be done with it.

The org.eclipse.jdt.core plug-in provides API for querying and manipulating Java programs. Its major capabilities include

*   Parsing and compiling Java source code<
*   Manipulating Java source files, using various object models<
*   Evaluating (running) code snippets
*   Searching, formatting, and invoking Content Assist on source files

  
The org.eclipse.jdt.ui plug-in is responsible for all UI elements of the Java development tools, including all the Java browsing views and the Java editor. The plug-in also provides API in a number of areas to allow other plug-ins to customize or extend the Java development tools:

*   Action classes for adding JDT actions to views in other plug-ins
*   Export code into JARs
*   Participation in refactorings
*   Java text editors and text hovers
*   Wizard components for creating Java projects and files

  
In addition to these two principal JDT plug-ins, a number of other plug-ins also provide APIs relating to Java development. The org.eclipse.jdt.debug plug-in provides support for launching and debugging Java programs. The org.eclipse.jdt.launching plug-in provides API for installing and configuring Java VMs and for looking up the source code corresponding to a Java library. Finally, the org.eclipse.jdt.unit plug-in has support for running JUnit tests and for programmatically monitoring the execution of JUnit tests.

See Also:
---------

 [JUnit](http://www.junit.org)

