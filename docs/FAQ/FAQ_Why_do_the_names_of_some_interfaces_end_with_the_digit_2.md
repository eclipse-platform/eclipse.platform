FAQ Why do the names of some interfaces end with the digit 2?
=============================================================

Owing to evolution in the use of Eclipse, some interfaces had to be extended with additional functionality. However, because of the dynamic nature of Eclipse, with many products relying on existing plug-ins, changing the signature of an interface requires that all the downstream plug-ins be not only recompiled but also fixed to implement the new methods required by the interface change.

This evolutionary side effect posed a big dilemma: cause all plug-ins to break and require intrusive enhancements from customers or introduce a totally new interface containing only the new functionality? Eclipse has chosen the second option. When you press Ctrl+Shift+T to locate a type and enter **I*2**, you will be shown a list of 20 such interfaces.

Note that additional overhead can occur. For instance, in class org.eclipse.ui.internal.PluginAction, special code needs to verify that the target implements the interface:

	   public void runWithEvent(Event event) {
	      ...
	      if (delegate instanceof IActionDelegate2) {
	         ((IActionDelegate2)delegate).runWithEvent(this, event);
	         return;
	      }
	      // Keep for backward compatibility with R2.0
	      if (delegate instanceof IActionDelegateWithEvent) {
	         ((IActionDelegateWithEvent) delegate).
	            runWithEvent(this, event);
	         return;
	      }
	      ...
	   }

The code gets messy owing to an early decision to name the interface differently. Although interfaces were added to Java to separate _types_ from their _implementations_, in the case of a successfully adopted platform such as Eclipse, they are not resilient to evolution. Changing them breaks too much existing code. If subclassing is used, the contract can easily be enhanced with a default implementation in the base class. The subclasses would not have to be changed or even recompiled.

