

FAQ How do I distinguish between internal and external JARs on the build path?
==============================================================================

The Java build path differentiates between internal JARs and external JARs. To find the file-system location for an internal JAR, the workspace path needs to be converted into a file-system location, as follows:

      IClasspathEntry entry = ...
      IPath path = entry.getPath();
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      IResource jarFile= workspace.getRoot().findMember(path);
      if (jarFile != null) {
         return jarFile.getLocation();
      } else {
         // must be an external JAR (or invalid classpath entry)
      }

See Also:
---------

[FAQ\_What\_is\_the\_difference\_between\_a\_path\_and\_a\_location?](./FAQ_What_is_the_difference_between_a_path_and_a_location.md "FAQ What is the difference between a path and a location?")

