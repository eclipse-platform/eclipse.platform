

FAQ What is a launch configuration?
===================================

A launch configuration is a description of how to launch a program. The program itself may be a Java program, another Eclipse instance in the form of a runtime workbench, a C program, or something else. Launch configurations are manifested in the Eclipse UI through **Run > Run...**.

Launching in Eclipse is closely tied to the infrastructure for debugging, enabling you to make the logical progression from support for launching to support for interactive debugging. This is why you will find launch configurations in the org.eclipse.debug.core plug-in.

For extensive documentation on how to add your own launch configuration, refer to _Platform Plug-in Developer Guide_ under **Programmer's Guide > Program Debug and Launch Support**. Also see the eclipse.org article, _We Have Lift-off: The Launching Framework in Eclipse_.

