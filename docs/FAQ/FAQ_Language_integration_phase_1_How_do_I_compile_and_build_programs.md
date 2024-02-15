FAQ/Language integration/phase 1: How do I compile and build programs?
======================================================================


Phase 1 of language integration with Eclipse focuses on compilation of source files and building projects. We can distinguish the following individual steps/approaches to obtain complete integration:

*   _Use no integration_. Edit and compile source code externally and load it into Eclipse. This makes it difficult for the eScript compiler to use such facilities as the PDE plug-in model, which is needed to discover all kinds of attributes about plug-ins to be written. In fact, using an external builder is impractical for any kind of serious integration. For more details, see [FAQ\_How\_do\_I\_load\_source\_files\_edited\_outside_Eclipse?](./FAQ_How_do_I_load_source_files_edited_outside_Eclipse.md "FAQ How do I load source files edited outside Eclipse?")

*   _Use external builders_. Edit your files with Eclipse, and use an Ant script to compile the source files. A problem is that information exchange between Eclipse and the external builder is severely limited; hence, the name _external builder_, of course. However, using an Ant script allows for some experimentation without the need to write a plug-in. For more details, see [FAQ\_How\_do\_I\_run\_an\_external\_builder\_on\_my\_source_files?](./FAQ_How_do_I_run_an_external_builder_on_my_source_files.md "FAQ How do I run an external builder on my source files?")

*   _Implement a compiler that runs inside Eclipse_. In other words, write the compiler in Java and run it in the same JVM as Eclipse runs in. One approach could be to add a PopupMenu command to eScript files in the Resource Navigator. Running the compiler in this fashion puts Eclipse in control. Files are built when Eclipse wants them and Eclipse does not need to react to changes from outside. For more details, see [FAQ\_How\_do\_I\_implement\_a\_compiler\_that\_runs\_inside\_Eclipse?](./FAQ_How_do_I_implement_a_compiler_that_runs_inside_Eclipse.md "FAQ How do I implement a compiler that runs inside Eclipse?")

*   _React to workspace changes._ Edit files by using Eclipse editors. Whenever the user saves a source file, you can be notified so that the file can be compiled. Integration is definitely improving but still is cumbersome as it does not integrate well with how Eclipse thinks about the way projects are built. For more details, see [FAQ\_How\_do\_I\_react\_to\_changes\_in\_source_files?](./FAQ_How_do_I_react_to_changes_in_source_files.md "FAQ How do I react to changes in source files?")

*   _Implement an Eclipse builder._ Builders are invoked on a project when any of its resources are changed or when the user manually requests a project to be rebuilt. Multiple builders can be registered on a project, and integration of a compiler into a build process is worth considering owing to its many benefits. For more details, see [FAQ\_How\_do\_I\_implement\_an\_Eclipse_builder?](./FAQ_How_do_I_implement_an_Eclipse_builder.md "FAQ How do I implement an Eclipse builder?")

After following these steps, you are _almost_ ready to focus on writing an editor. 
First, you have to look at [FAQ Language integration phase 2: How do I implement a DOM?](./FAQ_Language_integration_phase_2_How_do_I_implement_a_DOM.md "FAQ Language integration phase 2: How do I implement a DOM?")

