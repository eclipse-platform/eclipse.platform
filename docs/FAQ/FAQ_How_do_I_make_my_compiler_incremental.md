

FAQ How do I make my compiler incremental?
==========================================

The Eclipse Platform distinguishes between _full_ and _incremental_ builds. Incremental builds are much faster than a complete rebuild of a project because only the resources that have changed since the last build need to be considered for compilation.

Incremental builders are invoked each time a file is saved. To prevent a bad user experience, take special care to make the incremental builder as fast as possible. To make a compiler fast, it helps to understand that in most compilers little time is spent in compilation at all. Most time is spent resolving the context of the program, such as the build classpath.

In the case of eScript, two kinds of information need to be discovered from the target environment:

*   Starting with the Java classpath and the list of plug-ins referenced by a given script, the eScript compiler needs to find all possible external class types, as well as their methods and fields. This information is needed to determine whether a given input string refers to a class, an interface, a method, a field, or a local variable.

*   To facilitate the easy creation of the underlying Java class files for a given script, the eScript compiler, when it reads the contribution to a certain extension point, needs to interrogate the PDE to find out the class to extend or interface to implement.

  
The overhead of building the context is surprisingly constant for eScript and dwarfs the memory consumption needed for compiling a script. The scripts tend to be small, but the universe of plug-ins is large. The eScript compiler easily loads about 14,000 classes, simply to bind strings to type names. Rebuilding this contexts adds about three to four seconds to a compilation. The compilation of the script itself is less than a second, and is hardly noticeable.

By not discarding the context information after a compilation, performance of the next compilation run is greatly improved. The first compilation will be slow, but the next ones will be non-interruptive. However, note that optimization is always a time/space trade-off. The price we pay for faster compilation is about 15 MB of state that needs to be cached. JDT suffers from the same dilemma. Load a big Java project and close all perspectives, including the Java perspective, and the entire Java model with its thousands of classes is being held onto by the JDT.

Incremental builders that are run sporadically may be wise to run a timer and clean up their cache after a certain expiration time to free up heap memory used by the platform.

See Also:
---------

[FAQ Language integration phase 2: How do I implement a DOM?](./FAQ_Language_integration_phase_2_How_do_I_implement_a_DOM.md "FAQ Language integration phase 2: How do I implement a DOM?")

