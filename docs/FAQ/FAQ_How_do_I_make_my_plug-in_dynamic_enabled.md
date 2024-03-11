

FAQ How do I make my plug-in dynamic enabled?
=============================================

Most of the effort required to make a plug-in dynamic enabled can be summed up as doing what you should be doing anyway as part of good programming practice. Most importantly, to be dynamic enabled, your plug-in has to properly clean up after itself in the Plugin shutdown method. You need to keep in mind the following checklist for your plug-in's shutdown method.

*   If you have added listeners to notification services in other plug-ins, you need to remove them. This generally excludes any listeners on SWT controls created by your plug-in. When those controls are disposed of, your listeners are garbage collected anyway.

*   If you have allocated SWT resources, such as images, fonts, or colors, they need to be disposed of.

*   Any open file handles, sockets, or pipes must be closed.

*   Any metadata stored by other plug-ins that may contain references to your classes needs to be removed. For example, session properties stored on resources in the workspace need to be removed.

*   Other services that require explicit uninstall need to be cleaned up. For example, the runtime plug-in's adapter manager requires you to unregister any adapter factories that you have manually registered.

*   If your plug-in has forked background threads or jobs, they must be canceled and joined to make sure that they finish before your plug-in shuts down.

Prior to Eclipse 3.0, the consequences of failing to clean up properly were not as apparent as plug-ins were shut down only when the VM was about to exit. In a potentially dynamic world, the consequence of not being tidy is that your plug-in cannot be dynamic enabled.

See Also:
---------

*   [FAQ\_What\_is\_a\_dynamic_plug-in?](./FAQ_What_is_a_dynamic_plug-in.md "FAQ What is a dynamic plug-in?")
*   [FAQ\_How\_do\_I\_make\_my\_plug-in\_dynamic\_aware?](./FAQ_How_do_I_make_my_plug-in_dynamic_aware.md "FAQ How do I make my plug-in dynamic aware?")

