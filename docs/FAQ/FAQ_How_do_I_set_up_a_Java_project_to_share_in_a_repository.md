

FAQ How do I set up a Java project to share in a repository?
============================================================

A number of steps are needed to get a Java project properly set up to share with teammates in a repository.

*   Make sure that the .project and .classpath files are under version control. These files must be stored in the repository so that other users checking out the projects for the first time will get the correct type of project and will get the correct Java build path.

*   Avoid absolute paths in your .project and .classpath files.
    *   If you are using linked resources, make sure that they are created using _path variables_ (see the **Workbench > Linked Resources** preference page). If your project has references on its build path to external libraries, make sure that they are specified using a classpath variable (see the **Java > Build Path > Classpath Variables** preference page).
    *   If you're using dependency management tools such a OSGi/PDE, Maven/m2e or Gradle/BuildShip then the corresponding tools do use dynamic classpath containers so they don't reference any local path and you can safely commit the .project and .classpath too.

*   Make sure that the Java builder's output directory (conventionally called bin) is not under version control. In CVS, you can do this by creating a .cvsignore file in the project root directory containing the name of the output directory.

See Also:
---------

*   [FAQ What is the advantage of sharing the project file in a repository?](./FAQ_What_is_the_advantage_of_sharing_the_project_file_in_a_repository.md "FAQ What is the advantage of sharing the project file in a repository?")
*   [FAQ What is the function of the .cvsignore file?](./FAQ_What_is_the_function_of_the_cvsignore_file.md "FAQ What is the function of the .cvsignore file?")

