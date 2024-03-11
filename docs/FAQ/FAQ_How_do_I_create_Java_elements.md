

FAQ How do I create Java elements?
==================================

  

The Java model is made up of IJavaElement objects. Java elements represent all levels of a Java project, from the project itself all the way down to the types, methods, and fields. The Java model can be seen as a logical view of the underlying IWorkspace resource model.

  
An important characteristic of IJavaElements is that they are _handle objects_. This means that simply obtaining a Java element instance does not imply that the Java element exists. You can create IJavaElement handles for Java projects, packages, and compilation units that do not exist in your workspace. Conversely, IJavaElement instances do not always exist for all portions of the Java projects that do exist. IJavaElement handles are created lazily as they are requested by various clients. If multiple clients request handles on the same Java element, they will get equal, but not necessarily identical, handle objects.

  
The implication here is that creating a Java element has two meanings. You can create IJavaElement handles by asking the parent element for one. For example, IType.getMethod will return an IMethod handle but will not create that method in the file on disk. The JavaCore class also provides factory methods for creating Java elements for a given file, folder, or project in the workspace. For example, the following will create an ICompilationUnit handle for a given file handle:

      IFile file = ...;//a file handle
      ICompilationUnit unit = 
         JavaCore.createCompilationUnitFrom(file);

  
To create the contents on disk, you need to use the various create methods on the handle objects. For example, IType.createMethod will create a Java method on disk. Because creation will fail if such a method already exists, you should first use a method handle to find out whether the method already exists:

      IType type = ...;
      String body = "public String toString() {"+
         "return super.toString();}";
      IMethod method = type.'''getMethod'''("toString", new String\[0\]);
      if (!method.exists())
         method = type.createMethod(body, null, false, null);

  

See Also:
---------

[FAQ\_How\_are\_resources\_created?](./FAQ_How_are_resources_created.md "FAQ How are resources created?")

[FAQ\_What\_is\_the\_Java_model?](./FAQ_What_is_the_Java_model.md "FAQ What is the Java model?")

