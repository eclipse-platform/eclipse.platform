

FAQ How do I handle setup problems for a given builder?
=======================================================

When running a build in a runtime workbench, you might get the following message:

     !MESSAGE Skipping builder com.xyz.builder for project P.
        Either the builder is missing from the install, or it
        belongs to a project nature that is missing or disabled.

This message means that something is wrong with the builder plug-in or with the attribution of the builder specification in the .project file. The builder plug-in might load fine but still be broken, perhaps because it is missing an ID in the extension point declaration.

If everything else seems right to you, double-check the ID specified in the builder extension point. The ID should not be the plug-in ID of your builder, but rather the concatenation of the _plug-in ID_ and the _builder ID_. In other words, if the plug-ins ID is org.eclipse.escript.builder, and the ID of the builder is Builder, the builder ID reference in the .project file should be org.eclipse.escript.builder.Builder.

  

See Also:
---------

[FAQ How do I make my compiler incremental?](./FAQ_How_do_I_make_my_compiler_incremental.md "FAQ How do I make my compiler incremental?")

