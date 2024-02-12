FAQ Does Eclipse run on any Linux distribution?
===============================================

Each release of Eclipse is tested on a set of officially supported platforms:

*   [Eclipse Platform 4.7 (Oxygen)](https://www.eclipse.org/projects/project-plan.php?planurl=https://www.eclipse.org/eclipse/development/plans/eclipse_project_plan_4_7.xml#target_environments)
*   [Eclipse Platform 4.6 (Neon)](https://www.eclipse.org/projects/project-plan.php?planurl=https://www.eclipse.org/eclipse/development/plans/eclipse_project_plan_4_6.xml#target_environments)
*   [Eclipse Platform 4.5 (Mars)](https://www.eclipse.org/projects/project-plan.php?planurl=https://www.eclipse.org/eclipse/development/plans/eclipse_project_plan_4_5.xml#target_environments)

The latest releases should normally work fine on any recent Linux distribution. But the Linux graphical UI systems change fast and it is entirely possible that newer releases of Eclipse will not work on older distributions, and similarly older releases of Eclipse may not work on newer distributions.

Portability of Eclipse is defined mainly by the underlying Java runtime (Eclipse 4.6 and later needs a Java 8 runtime) and by what platform SWT runs on, as all graphical UI in Eclipse are based on SWT.

For historical interest, earlier versions of Eclipse have also been compiled with gcj [gcj](http://www.klomp.org/mark/classpath/eclipse-gnome-gij.png) and even made to run on .Net, using IKVM on the CLR or Mono through the amazing work of Jeroen Frijters.

See Also:
---------

*   [The Eclipse download site ](https://eclipse.org/downloads)
*   [IKVM](http://www.ikvm.net)

