

FAQ How do I use a model reconciler?
====================================

  

Advanced text editors, such as those for programming languages or Web development, often have an underlying object model that represents the elements being shown in the editor. Such models are often used for semantic manipulation, such as refactoring, or for querying by other tools. As the user types in such an editor, the text can become out of sync with this underlying model. For example, an HTML editor may have a model that contains information about hyperlinks in and out of the document. As the user edits the document, adding or removing links and anchors, this model will invariably become out of date. If another tool makes a query on this model while it is out of date, invalid results may occur.

  
When the editor is operating on a file in the workspace, one approach to solving this problem is to use an incremental project builder to update the model. With autobuild enabled, the model would be updated every time the user saved the document. However, if autobuild is turned off, this strategy can result in long periods of time in which the model is out of date. If the model update is a costly computation, this might be the only practical trade-off. By turning autobuild off, the user can control the build frequency.

  
For a lighter-weight model that can be updated with little overhead, it is preferable to update the model more frequently. This is where a text editor _reconciler_ comes into play. When a reconciler is installed on an editor, a queue is created to record all the changes that occur. Each change is represented as a DirtyRegion object, and the regions are added to a DirtyRegionQueue. The reconciler removes items from the queue and updates the model accordingly. If several edits occur in the document before the reconciler processes them, the DirtyRegion objects in the queue will merge where appropriate. For example, continuously typing in an editor will create one large dirty region rather than individual dirty regions for each character pressed. The reconciler can analyze the DirtyRegion objects to see what portions of the text have been invalidated, allowing it to perform more optimized updates.

  
A reconciler can be installed by overriding the getReconciler method declared in your subclass of SourceViewerConfiguration. You can choose from a couple of built-in reconcilers, or you can implement the IReconciler interface directly. Most of the time, you can use MonoReconciler, an implementation that does not distinguish between reconciling in different document partitions. This reconciler runs in a low-priority background thread, allowing multiple changes to be added to the queue before processing them. Performing the reconciliation asynchronously allows the user to continue editing the document while it is being reconciled, although this results in a short period in which the document will be out of date.

  
Note that reconcilers are generally not an adequate replacement for builders but can play a complementary role. For example, in the JDT plug-ins, the reconciler performs a parse of the class as the user makes changes. This parser gathers enough information to update the Java model, allowing accurate content assist, refactoring, and other common operations. When the user saves and builds the file, an incremental builder performs a full compilation, generating class files and recompiling any other files affected by the change. This way, the expensive processing is deferred, but the domain model always stays up to date.

  

See Also:
---------

[FAQ Language integration phase 2: How do I implement a DOM?](./FAQ_Language_integration_phase_2_How_do_I_implement_a_DOM.md "FAQ Language integration phase 2: How do I implement a DOM?")

