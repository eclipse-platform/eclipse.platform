

FAQ Where is the workspace local history stored?
================================================

Every time you modify a file in Eclipse, a copy of the old contents is kept in the local history. At any time, you can compare or replace a file with any older version from the history. Although this is no replacement for a real code repository, it can help you out when you change or delete a file by accident. Local history also has an advantage that it wasn't really designed for: The history can also help you out when your workspace has a catastrophic problem or if you get disk errors that corrupt your workspace files. As a last resort, you can manually browse the local history folder to find copies of the files you lost, which is a bit like using Google's cache to browse Web pages that no longer exist. Each file revision is stored in a separate file with a random file name inside the history folder. The path of the history folder inside your workspace is

      .metadata/.plugins/org.eclipse.core.resources/.history/

You can use your operating system's search tool to locate the files you are looking for. Although not the prettiest backup system, it sure beats starting over from scratch!

