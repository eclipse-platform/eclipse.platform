

FAQ How do I make the workbench shutdown?
=========================================

You can force the workbench to exit by calling the close method on IWorkbench. This is the same behavior as would occur if the end user had selected **File > Exit**. If you want the workbench to close and immediately restart-for example, if a new plug-in has been installed and you have a plug-in that does not support dynamic plug-ins-you can instead call the restart method.

Note that although API exists for exiting and restarting the workbench, this measure is fairly drastic and should not be employed lightly. As support for dynamically installed plug-ins increases, it will become increasingly unacceptable to restart the workbench to install new plug-ins. At the very least, before calling either of these methods, you should prompt the user as to whether he or she want to exit.

