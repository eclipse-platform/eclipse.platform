

FAQ How does OSGi and the new runtime affect me?
================================================

Just when you thought you were beginning to understand how the Eclipse kernel worked, those pesky Eclipse developers replaced it all for Eclipse 3.0. The kernel is now built on another Java component framework, the Open Services Gateway initiative (OSGi). The reasons for this convergence between Eclipse and OSGi are manifold. The two frameworks had many similarities to begin with, and each framework had many features that the other lacked.

By bringing the two together, Eclipse gained the infrastructure for many new features, especially dynamic addition and removal of plug-ins and a more robust security model. Eclipse in turn has a powerful declarative model-extensions and extension points-that OSGi lacked, in addition to more advanced support for multiple versions, fragments, a commercial-quality open source implementation, and great tooling support. Rather than creating a derivative OSGi++, the Eclipse community is contributing a number of important Eclipse features back into the OSGi specification, paving the way for better interoperability between the two frameworks. All in all, it's what the marketing types like to call a win-win situation.

Now, to the question of how plug-ins are affected: The new runtime is 100 percent backward compatible with the runtime that existed in all versions before Eclipse 3.0. Plug-ins written prior to 3.0 will continue to run without requiring any modification. When you port a plug-in to 3.0, you can still make use of the old runtime API by explicitly importing the backward-compatibility layer, which is found in a separate plug-in. Although the boot and runtime plug-ins were imported automatically prior to Eclipse 3.0, the runtime must now be imported explicitly. The new runtime compatibility plug-in contains the deprecated portions of the API from the boot and runtime plug-ins and also exports the new runtime plug-in. In short, all you now have to import is the new runtime compatibility plug-in, and you will get access to both the new runtime API and the old. The following example from a plugin.xml file imports both the old and new runtimes:

      <requires>
         <import plugin="org.eclipse.core.runtime.compatibility"/> 
      </requires>

Apart from that one change, you can continue using runtime facilities as you did prior to Eclipse 3.0. Over time, more elements of the old runtime will likely become deprecated, and plug-ins will begin to use the equivalent OSGi APIs instead. However, for release 3.0, the focus is on getting the technology in place with minimal disruption to the rest of the platform. For now, the fact that Eclipse is running on OSGi is an implementation detail that will not significantly affect you.

See Also:
---------

*   [FAQ What is a dynamic plug-in?](./FAQ_What_is_a_dynamic_plug-in.md "FAQ What is a dynamic plug-in?")
*   [The OSGi Web site](http://www.osgi.org)

