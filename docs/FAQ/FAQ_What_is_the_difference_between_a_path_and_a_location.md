FAQ What is the difference between a path and a location?
=========================================================

In general, the term _location_ represents physical file system paths, and _path_ represents a resource's logical position within the workspace. Many people are confused by this distinction, as both terms are represented by using the IPath data type.

IPath is in fact an abstract data structure that represents a series of slash-separated strings. Many people assume that paths always correspond to a file-system location, but this is not always the case.

In fact, IPath objects are used in a variety of contexts for locating an object within a tree-like hierarchy. For example, a resource path returned by IResource.getFullPath, represents the position of a resource within the workspace tree. The first segment is the name of the project, the last segment is the name of the file or folder, and the middle segments are the names of the parent folders in order between the project and the file or folder in question. This doesn't always match the path of the resource in the file system!

The file-system location of a resource, on the other hand, is returned by the method IResource.getLocation. Methods on IContainer and IWorkspaceRoot can help you convert from one to the other. The following code converts from a path to a location:

      IPath path = ...;
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      IWorkspaceRoot root = workspace.getRoot();
      IResource resource = root.findMember(path);
      if (resource != null) {
         location = resource.getLocation();
      }

Note that in some situations, a resource can have a null location. In particular, resources whose project doesn't exist and linked resources that are relative to a nonexistent path variable will have a null location.

Here is the converse code to convert from a location to the workspace paths that correspond to it:

      IPath location = ...;
      IFile[] files = root.findFilesForLocation(location);
      IFolder[] folders = root.findContainersForLocation(location);
      if (files.length > 0) {
         for (int i = 0; i < files.length; i++)
            path = files[i].getLocation();
      } else {
         for (int i = 0; i < folders.length; i++)
            path = folders[i].getLocation();
      }

As this snippet shows, a single file-system location can correspond to multiple resources. This is true because linked resources can point to locations inside other projects. Of course, the same file-system location can't correspond to both files and folders at the same time.

When using API from the resources plug-in that involve IPath objects, always read the API javadoc carefully to see whether it deals with paths or locations. These different types of paths must never be mixed or used interchangeably, as they represent completely different things.

