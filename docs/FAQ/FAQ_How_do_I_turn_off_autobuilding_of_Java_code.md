

FAQ How do I turn off autobuilding of Java code?
================================================

JDT uses the autobuild facilities provided by the platform. If a resource changes, the platform checks the project description file (see .project in your projects). When the file contains a reference to the Java builder, the builder gets notified of the change and will then compile the Java source file and its dependents. The following project description file snippet shows that the Java builder is associated with the project:

      <buildSpec>
         <buildCommand>
            <name>org.eclipse.jdt.core.javabuilder</name>
            <arguments>
            </arguments>
         </buildCommand>
      </buildSpec>

If a workspace gets large-say, tens of thousands of files-the process of checking each project, activating all registered builders, and discovering whether anything needs to be rebuilt owing to a single resource save may have a considerable impact on the responsiveness of the workbench. In these situations, autobuild can be turned off through **Window > Preferences > General > Workspace >** **Build automatically**.

  

Even for smaller workspaces, turning off autobuilding may be a useful feature. For instance, when importing a large number of plug-ins from CVS, it may make sense to turn off autobuilding first. After all files are checked out, autobuilding is turned on again, and all pending builds are run in one swoop.

  

  

See Also:
---------

[FAQ\_Where\_can\_I\_find\_information\_about\_writing\_builders?](./FAQ_Where_can_I_find_information_about_writing_builders.md "FAQ Where can I find information about writing builders?")

  
[FAQ\_How\_do\_I\_implement\_an\_Eclipse_builder?](./FAQ_How_do_I_implement_an_Eclipse_builder.md "FAQ How do I implement an Eclipse builder?")

