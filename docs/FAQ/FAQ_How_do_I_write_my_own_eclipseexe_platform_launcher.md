

FAQ How do I write my own eclipseexe platform launcher?
=======================================================

Most of the time, it is not necessary to write your own customized native launcher. The default launcher supports a large number of command-line arguments, allowing you to customize the splash screen, plug-in configuration, JVM, and much more. In some cases, you may want to wrap the native launcher in another launcher to prime the set of command-line arguments passed to the default launcher.

  
If you do need to write your own native launcher, the obvious place to start is by looking at the source code for the Eclipse launcher. This source is found in the Eclipse CVS repository in the org.eclipse.equinox.executable project.

  

See Also:
---------

[FAQ\_How\_do\_I\_run_Eclipse?](./FAQ_How_do_I_run_Eclipse.md "FAQ How do I run Eclipse?")

[FAQ\_Where\_can\_I\_find\_the\_Eclipse_plug-ins?](./FAQ_Where_can_I_find_the_Eclipse_plug-ins.md "FAQ Where can I find the Eclipse plug-ins?")

