

FAQ When should I use refreshLocal?
===================================

Resources become out of sync with the workspace if they are changed directly in the file system without using Eclipse workspace API. If your plug-in changes files and folders in this way, you should manually synchronize the workspace with the file system using the method IResource.refreshLocal. Some common situations where this is necessary include:

*   When files are manipulated using java.io or java.nio, such as writing to a FileOutputStream.
*   When your plug-in calls an external editor that is not Eclipse-aware.
*   When you do file I/O using your own natives.
*   When you launch external builders, tools, or Ant scripts that modify files in the workspace.

  
Here is an example snippet that sets the contents of a file using a FileOutputStream, and then uses refreshLocal to synchronize the workspace. This example is a bit contrived, because you could easily use the workspace API in this case. Imagine that the actual file manipulation is buried in some third-party library that you can't change, and this code makes more sense.

      private void externalModify(IFile iFile) throws ... {
         java.io.File file = iFile.getLocation().toFile();
         FileOutputStream fOut = new FileOutputStream(file);
         fOut.write("Written by FileOutputStream".getBytes());
         iFile.refreshLocal(IResource.DEPTH_ZERO, null);
      }

You might reasonably ask why the workspace does not refresh out of sync files automatically. The answer in Eclipse 3.0 is that it can, with some caveats. The problem is that there is no way to do this efficiently in a platform-neutral way. Some operating systems have callback mechanisms that send notifications when there are changes to particular sections of the file system, but this support doesn't exist on all operating systems that Eclipse runs on. The only way to perform automatic refresh across all platforms is to have a background thread that periodically polls the file system for changes. This clearly can have a steep performance cost, especially since workspaces might easily contain tens of thousands of files.

  
The solution in Eclipse 3.0 is to have an auto-refresh capability that is turned off by default. On platforms that have efficient native support for change notification, this can be quite fast. On other platforms, change notification can be very slow. Users who have a need to modify files externally on a regular basis may gladly pay the performance cost to get automatic workspace refresh. Users who do not make external modifications to files, can leave auto-refresh off and avoiding paying a performance penalty for a feature they do not need. This follows the general performance rule of only introducing a performance hit if and when it is actually needed. Auto-refresh can be turned on by checking **Refresh workspace automatically** on the **Workbench** preference page.

  
The down side of auto-refresh is that programmatic clients of the workspace API still need to check for and defend against out of sync resources. Resources can be out of sync if auto-refresh is turned off, and they can even be out of sync when auto-refresh is turned on. If you are programmatically modifying files in the workspace externally, you should still perform a refreshLocal to make sure files come back in sync as quickly as possible.

  

