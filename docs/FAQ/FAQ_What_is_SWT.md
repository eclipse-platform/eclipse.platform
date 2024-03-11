

FAQ What is SWT?
================

Standard Widget Toolkit (SWT) is the UI toolkit used by the Eclipse Platform and most other Eclipse projects. Its stated goal, according to the SWT home page, is to provide efficient, portable access to the user-interface features of the operating systems on which it is implemented. Its goal is not to provide a rich user-interface design framework but rather the thinnest possible user-interface API that can be implemented uniformly on the largest possible set of platforms while still providing sufficient functionality to build rich graphical user interface (GUI) applications.

  

SWT is implemented by creating thin native wrappers for the underlying operating system's user-interface APIs. The bulk of SWT's source is Java code, which defers as much work as possible to the appropriate operating system native. Thus, when you create a tree widget in SWT, it calls through to the operating system to create a native tree widget. The result is that SWT applications tend to look and behave exactly like native applications on the system they are running on. No Java emulation is done at all, except if no native API will satisfy the needs of the SWT API. Thus, if a platform does not provide a tree widget, SWT will implement an emulated tree widget in Java.

  

SWT does not make use of AWT or any other Java tool kit to implement its functionality. SWT also makes minimal use of Java class libraries, thus allowing it to be run with older JDKs or restricted class libraries on handheld computers. Implementations of SWT are currently available on the following platforms:

  

*   Win32
*   Linux GTK
*   MacOS
