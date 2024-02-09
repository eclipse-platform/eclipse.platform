

FAQ Where are project build specifications stored?
==================================================

A project is built according to the specifications defined in its .project file. To see the .project file for a given project, click on the **Menu** toggle in the Package Explorer's toolbar, select **Filters...**, and deselect **.\* files**. Open the .project file. The .project file for a plug-in should look similar to this:

    <?xml version="1.0" encoding="UTF-8"?>
    <projectDescription>
        <name>org.eclipse.escript.builder</name>
        <projects>
            ...
        </projects>
        <buildSpec>
            <buildCommand>
                <name>org.eclipse.jdt.core.javabuilder</name> 
                <arguments> </arguments>
            </buildCommand>
            <buildCommand>
                <name>org.eclipse.pde.ManifestBuilder</name> 
                <arguments> </arguments>
            </buildCommand>
            <buildCommand>
                <name>org.eclipse.pde.SchemaBuilder</name> 
                <arguments> </arguments>
            </buildCommand>
        </buildSpec>
        <natures>
            <nature>org.eclipse.pde.PluginNature</nature>
            <nature>org.eclipse.jdt.core.javanature</nature>
        </natures>
    </projectDescription>

  

See Also:
---------

[FAQ How do I add a builder to a given project?](./FAQ_How_do_I_add_a_builder_to_a_given_project.md "FAQ How do I add a builder to a given project?")

