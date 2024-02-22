

FAQ How do I accommodate project layouts that don't fit the Eclipse model?
==========================================================================

Let's say that you are new to Eclipse, but have some existing projects with file system layouts that cannot be changed. Perhaps you have other tools or build processes that require your projects to be laid out in a certain way. Because Eclipse also has expectations about how projects are laid out on disk, you can run into problems when you try to get started in Eclipse with your existing projects.

The Eclipse Help contains a section titled "**Project Configuration Tutorial**" that can help you understand how to configure a variety of types of project layouts. It is in the **Java Development User Guide**, under the heading **Getting Started**.

### Linked Resources

In release 2.1, Eclipse introduced the notion of _linked resources_ to help deal with problems like this. Linked resources can refer to files or folders anywhere in your file system, even inside other Eclipse projects. Using linked resources, you can cobble together a project from files and folders that are scattered all over your file system. The link descriptions are stored in the file called .project inside your project content area. If you share this file with a repository, other users will be able to load the project and get all the links reconstructed automatically in their workspace. If you do not want to hard-code particular file system paths, you can define linked resources relative to workspace path variables. Path variables can be added or changed from the **Workbench > Linked Resources** preference page.

For more information on using linked resources, see the good general introduction in the _Workbench User Guide_, under **Concepts > Workbench > Linked resources**. The _Java Development User Guide_ also has an excellent tutorial that helps you get started with various types of project configurations. Look under **Getting Started > Project configuration tutorial**. Information on how to define linked resources programmatically is found in the _Platform Plug-in Developer Guide_, under **Programmer's Guide > Resource and workspace API > Linked Resources**.

