

FAQ How do I make my plug-in dynamic aware?
===========================================

Dynamic awareness requires extra steps that were not required prior to the introduction of dynamic plug-ins. Dynamic awareness requires that you remove all references to classes defined in other plug-ins when those plug-ins are removed from the system. In particular, if your plug-in defines extension points that load classes from other plug-ins-executable extensions-you need to discard those references when other plug-ins are dynamically removed. The extension registry allows you to add a listener that notifies you when extensions are being added or removed from the system. If your plug-in maintains its own cache of extensions that are installed on your extension point, your listener should update this cache for each added or removed extension.

The following is an example of a simple class that maintains its own cache of the set of extensions installed for a given extension point. This example is a bit contrived as simply caching the extension objects has no value. Typically, your plug-in will process the extensions to extract useful information and possibly load one or more classes associated with that extension. The basic structure of this cache example is as follows:

      public class ExtCache implements IRegistryChangeListener {
         private static final String PID = "my.plugin";
         private static final String PT_ID = 
            PID + "." + "extension.point";
         private final HashSet extensions = new HashSet();
         ...
      }

The extensions field stores the set of installed extensions for a particular extension point.

The cache has a startup method that loads the initial set of extensions and then adds an extension registry listener in order to be notified of future changes:

      public void startup() {
         IExtensionRegistry reg = Platform.getExtensionRegistry();
         IExtensionPoint pt = reg.getExtensionPoint(PT_ID);
         IExtension[] ext = pt.getExtensions();
         for (int i = 0; i < ext.length; i++)
            extensions.add(ext[i]);
         reg.addRegistryChangeListener(this);
      }

The class implements the IRegistryChangeListener interface, which has a single method that is called whenever the registry changes:

      public void registryChanged(IRegistryChangeEvent event) {
         IExtensionDelta[] deltas = 
                           event.getExtensionDeltas(PID, PT_ID);
         for (int i = 0; i < deltas.length; i++) {
            if (deltas[i].getKind() == IExtensionDelta.ADDED)
               extensions.add(deltas[i].getExtension());
            else
               extensions.remove(deltas[i].getExtension());
         }
      }

This class is now dynamic aware but is not yet dynamic enabled; that is, the class does not yet support itself being dynamically removed. The final step is to implement a shutdown method that clears all values from the cache and removes the listener from the extension registry:

      public void shutdown() {
         extensions.clear();
         IExtensionRegistry reg = Platform.getExtensionRegistry();
         reg.removeRegistryChangeListener(this);
      }

This shutdown method must be called from the shutdown method of the plug-in that defines the cache. For the complete source code of this example, see the ExtCache class in the FAQ Examples plug-in.

Note that not only extensions points acquire and maintain references to classes defined in other plug-ins. You need to be especially aware of static fields and caches that contain references to objects whose class is defined in other plug-ins.

If you hold onto classes defined in other plug-ins through different mechanisms, you also need to discard those references when those other plug-ins are removed.

See Also:
---------

*   [FAQ What is a dynamic plug-in?](./FAQ_What_is_a_dynamic_plug-in.md "FAQ What is a dynamic plug-in?")
*   [FAQ How do I make my plug-in dynamic enabled?](./FAQ_How_do_I_make_my_plug-in_dynamic_enabled.md "FAQ How do I make my plug-in dynamic enabled?")

