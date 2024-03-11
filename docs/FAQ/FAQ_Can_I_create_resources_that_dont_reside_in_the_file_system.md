

FAQ Can I create resources that don't reside in the file system?
================================================================

No. Resources are strictly a file system-based model. They cannot be used to represent files on a remote server or in a database. Although this prevents a large amount of the platform's functionality from being used for nonlocal applications, there were strong architectural reasons for taking this approach. A principal design goal of Eclipse from the start was to enable it to interoperate smoothly with other tools. Because most tools operate on resources in the file system, this is an important medium for interaction between applications. If Eclipse were built on an abstract resource model with no necessary connection with the file system, interaction between Eclipse plug-ins and non-Eclipse-aware tools would be very difficult.

  
Having said that, nothing requires you to use IResource as your model layer. Almost none of the base platform is tied to resources, and the text-editing framework can also operate on non-IResource models. If you want to build plug-ins that operate on files located remotely, you can define your own model layer to represent them and build views and editors that interact with that remote model rather than with the strictly local IResource model. In Eclipse 3.0, the RCP has no relationship at all with the IResource-based workspace. Only the Eclipse IDE plug-ins still retain a dependency on this model.

  
If you use resources, you will be tied to the local file system, but you will have a common layer that allows you to interoperate seamlessly with other tools. If you don't want to be tied to the local file system, you can build your own model at the expense of lost integration with plug-ins that are not aware of it. Seamless integration of disparate tools based on a completely abstract resource model is a lofty idea, but like many lofty ideas, it is one that has yet to take flight.

