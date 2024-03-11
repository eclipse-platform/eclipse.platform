

FAQ How do I create a Java project?
===================================

Several steps are required to create and properly initialize a Java project. Start by creating and opening an IProject:

      String name = "MyProject";
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      IWorkspaceRoot root= workspace.getRoot();
      IProject project= root.getProject(name);
      project.create(null);
      project.open(null);

Next, you need to add the Java nature to the project. This in turn will cause the Java builder to be added to the project:

      IProjectDescription desc = project.getDescription();
      desc.setNatureIds(new String[] {
         JavaCore.NATURE_ID});
      project.setDescription(desc);

Next, you must set the Java builder's output folder, typically called bin. This is where the Java builder will place all compiled *.class files:

      IJavaProject javaProj = JavaCore.create(project);
      IFolder binDir = project.getFolder("bin");
      IPath binPath = binDir.getFullPath();
      javaProj.setOutputLocation(binPath, null);

Finally, you need to set the project's classpath, also known as the build path. You will need to minimally create a classpath entry that points to the Java runtime library, rt.jar, and additional entries for any other libraries and projects that the project requires:

      String path = "c:\\jre\\lib\\rt.jar";
      IClasspathEntry cpe= JavaCore.newLibraryEntry(
         path, null, null);
      javaProj.setRawClasspath(new IClasspathEntry[] {cpe});

See Also:
---------

[FAQ When does PDE change a plug-in's Java build path?](FAQ_When_does_PDE_change_a_plug-ins_Java_build_path.md)

[FAQ\_How\_are\_resources\_created?](./FAQ_How_are_resources_created.md "FAQ How are resources created?")

[FAQ\_Why\_should\_I\_add\_my\_own\_project\_nature?](./FAQ_Why_should_I_add_my_own_project_nature.md "FAQ Why should I add my own project nature?")

