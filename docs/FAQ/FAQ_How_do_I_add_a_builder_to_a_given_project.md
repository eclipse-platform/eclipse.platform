

FAQ How do I add a builder to a given project?
==============================================

To register the eScript builder for a given project, add the builder to the project's build specification as follows:

      private void addBuilder(IProject project, String id) {
         IProjectDescription desc = project.getDescription();
         ICommand[] commands = desc.getBuildSpec();
         for (int i = 0; i < commands.length; ++i)
            if (commands[i].getBuilderName().equals(id))
               return;
         //add builder to project
         ICommand command = desc.newCommand();
         command.setBuilderName(id);
         ICommand[] nc = new ICommand[commands.length + 1];
         // Add it before other builders.
         System.arraycopy(commands, 0, nc, 1, commands.length);
         nc[0] = command;
         desc.setBuildSpec(nc);
         project.setDescription(desc, null);
      }

Alternatively, you could edit the project description directly on disk by modifying the .project file:

      <buildCommand>
         <name>org.eclipse.escript.builder.Builder</name> 
         <arguments> </arguments>
      </buildCommand>

A builder is normally added to a project in the project creation wizard but can be added later on.

  

See Also:
---------

[FAQ How do I implement an incremental project builder?](./FAQ_How_do_I_implement_an_incremental_project_builder.md "FAQ How do I implement an incremental project builder?")

