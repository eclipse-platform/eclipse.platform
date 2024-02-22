

FAQ How are resources created?
==============================

The workspace is manipulated using _resource handles_. Resource handles are lightweight pointers to a particular project, folder, or file in the workspace. You can create a resource handle without creating a resource, and resources can exist regardless of whether any handles exist that point to them. To create a resource, you first have to create a resource handle and then tell it to create the resource. The following snippet uses resource handles to create a project, a folder, and a file.

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IProject project  = root.getProject("MyProject");
        IFolder folder = project.getFolder("Folder1");
        IFile file = folder.getFile("hello.txt");
        //at this point, no resources have been created
        if (!project.exists()) project.create(null);
        if (!project.isOpen()) project.open(null);
        if (!folder.exists()) 
            folder.create(IResource.NONE, true, null);
        if (!file.exists()) {
            byte[] bytes = "File contents".getBytes();
            InputStream source = new ByteArrayInputStream(bytes);
            file.create(source, IResource.NONE, null);
        }

This example defensively checks that the resource doesn't already exist before trying to create it. This kind of defensive programming is a good idea because an exception is thrown if you try to create a resource that already exists. This way, the example can be run more than once in the same workspace without causing an error. The null parameters to the creation methods should be replaced by a progress monitor in a real application.

