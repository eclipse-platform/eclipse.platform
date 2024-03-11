

FAQ How do I react to changes in source files?
==============================================

Register a workspace resource change listener. Inside our eScript plug-in class, we call

      IResourceChangeListener rcl = new IResourceChangeListener() {
         public void resourceChanged(IResourceChangeEvent event) {
            IResource resource = event.getResource();
            if (resource.getFileExtension().equals("escript")) {
               // run the compiler
            }
         }
      };
      ResourcesPlugin.getWorkspace().addResourceChangeListener(rcl);

We will be notified whenever the file is changed in the workspace under Eclipse's control. Changes made to the files outside Eclipse will not be detected unless the workspace is being explicitly refreshed. Alternatively, a separate worker thread could be used to monitor the file system and inform Eclipse of any files having changed.

If the source files are edited with an Eclipse text editor, this scenario will work, and files can be compiled as soon as saved. We certainly are on the right path for our language integration because all editing and compilation are done inside Eclipse on the same Java VM.

Even though we are more integrated than when running an external builder, reacting to workspace changes remains cumbersome, and a much better approach is to use an integrated builder.

See Also:
---------

Tracking resource changes (See **Platform Plug-in Developer's Guide**)

Eclipse online article How You've Changed! Responding to resource changes in the Eclipse workspace

