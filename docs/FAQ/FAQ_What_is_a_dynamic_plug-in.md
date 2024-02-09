

FAQ What is a dynamic plug-in?
==============================

Prior to Eclipse 3.0 the platform had to be restarted in order for added, removed, or changed plug-ins to be recognized. This was largely owing to the fact that the plug-in registry was computed statically at start-up, and no infrastructure was available for changing the registry on the fly. In Eclipse 3.0, plug-ins can be added or removed dynamically, without restarting. Dynamicity, however, does not come for free. Roughly speaking, plug-ins fall into four categories of dynamicity.

*   _Nondynamic_ plug-ins do not support dynamicity at all. Plug-ins written prior to Eclipse 3.0 are commonly in this category. Although they can still often be dynamically added or removed, there may be unknown side effects. Some of these plug-ins' classes may still be referenced, preventing the plug-ins from being completely unloaded. A nondynamic plug-in with extension points will typically not be able to handle extensions that are added or removed after the plug-in has started.

*   _Dynamic-aware_ plug-ins support other plug-ins being dynamic but do not necessarily support dynamic addition or removal of themselves. The generic workbench plug-in falls into this category. It supports other plug-ins that supply views, editors, or other workbench extensions being added or removed, but it cannot be dynamically added or removed itself. It is generally most important for plug-ins near the bottom of a dependency chain to be dynamic aware.

*   _Dynamic-enabled_ plug-ins support dynamic addition or removal of themselves, but do not necessarily support addition or removal of plug-ins they interact with. A well-behaved plug-in written prior to Eclipse 3.0 should already be dynamically enabled. Dynamic enablement involves following good programming practices. If your plug-in registers for services, adds itself as a listener, or allocates operating system resources, it should always clean up after itself in its inherited Plugin.shutdown method. A plug-in that does this consistently is already dynamic enabled. It is most important for plug-ins near the top of a dependency chain to be dynamic enabled.

*   _Fully dynamic_ plug-ins are both dynamic aware and dynamic enabled. A system in which all plug-ins are fully dynamic is very powerful, as any individual plug-in can be added, removed, or upgraded in place without taking the system down. In fact, such a system would never have any reason to shut down as it could heal itself of any damage or bug by doing a live update of the faulty plug-in. OSGi, the Java component architecture that is used to implement the Eclipse kernel, is designed around this goal of full dynamicity.

The dynamicity of a plug-in depends on the dynamic capabilities of the plug-ins it interacts with. Even if a plug-in is fully dynamic, it may not be possible to cleanly remove it if a plug-in that interacts with it is not dynamic aware. As long as someone maintains a reference to a class defined in a plug-in, that plug-in cannot be completely removed from memory.

When a request is made to dynamically add or remove a plug-in, the platform will always make a best effort to do so, regardless of the dynamic capabilities of that plug-in. However, there may be unexpected errors or side effects if all plug-ins in the system that reference it are not well-behaved, dynamic citizens.

See Also:
---------

*   [FAQ How do I make my plug-in dynamic enabled?](./FAQ_How_do_I_make_my_plug-in_dynamic_enabled.md "FAQ How do I make my plug-in dynamic enabled?")
*   [FAQ How do I make my plug-in dynamic aware?](./FAQ_How_do_I_make_my_plug-in_dynamic_aware.md "FAQ How do I make my plug-in dynamic aware?")

