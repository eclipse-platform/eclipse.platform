

FAQ How do I implement a compiler that runs inside Eclipse?
===========================================================

First, a compiler like the eScript compiler should, of course, be written as a plug-in. In other words, the compiler will have dependent plug-ins, such as the core.resources plug-in to read source files and class files and to write resulting Java class files. The compiler has preferences, so it declares a preference page. Furthermore, to discover all kinds of structural information about the target platform, the compiler leans heavily on PDE to help out.

More important the compiler can itself be a publisher of API and can contribute a set of extension points to which other plug-ins can define extension points.

By making the compiler a plug-in, it automatically runs inside Eclipse, and it can keep information cached for later compilation runs. For instance, it can be quite expensive to compute the full list of classes available for class-name resolution if the compiler sees the word Shell and needs to determine that a reference is made to org.eclipse.swt.widgets.Shell. Such metainformation has to be recomputed each time the compiler is executed in an external process, such as when run from a command-line compiler. Keeping the metadata in memory allows for _incremental_ compilation strategies, greatly improving the user experience and reinforcing the feeling of _integration_ with the platform. Later, we discuss how to implement a compiler in Eclipse.

See Also:
---------

*   [FAQ\_How\_do\_I\_implement\_an\_Eclipse_builder?](./FAQ_How_do_I_implement_an_Eclipse_builder.md "FAQ How do I implement an Eclipse builder?")

