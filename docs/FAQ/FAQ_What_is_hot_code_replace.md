FAQ What is hot code replace?
=============================

Hot code replace (HCR) is a debugging technique whereby the Eclipse Java debugger transmits new class files over the debugging channel to another JVM. In the case of Eclipse development, this also applies to the VM that runs the runtime workbench. The idea is that you can start a debugging session on a given runtime workbench and change a Java file in your development workbench, and the debugger will replace the code in the receiving VM while it is running. No restart is required, hence the reference to "hot".

HCR has been specifically added as a standard technique to Java to facilitate experimental development and to foster iterative trial-and-error coding. HCR only works when the class signature does not change; you cannot remove or add fields to existing classes, for instance. However, HCR can be used to change the body of a method. HCR is reliably implemented only on 1.4.1 VMs and later, or using any version of the IBM J9 VM. J9 is available in IBM products such as Websphere Studio Device Developer.

If HCR does not work for you even in a simple Java application and you have confirmed that you are running the application on a supported VM (taking note that the JVM that runs Eclipse may not be the same as the JVM that is running your Java application), you may not have automatic building turned on. Make sure that 'Project > Build Automatically' is checked.


