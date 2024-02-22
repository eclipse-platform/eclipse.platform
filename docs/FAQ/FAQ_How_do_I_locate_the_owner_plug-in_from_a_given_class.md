

FAQ How do I locate the owner plug-in from a given class?
=========================================================

You can't. Some known hacks were used prior to Eclipse 3.0 to obtain this information, but they relied on implementation details that were not strictly specified. For example, you could obtain the class's class loader, cast it to PluginClassLoader, and then ask the class loader for its plug-in descriptor. This relied on an assumption about the class loading system that is subject to change and, in fact, has changed in Eclipse 3.0. The correct answer to this question is that there is no way to reliably determine this information. If you are exploiting knowledge of the Eclipse runtime's implementation to obtain this information, expect to be foiled when the runtime implementation changes.

