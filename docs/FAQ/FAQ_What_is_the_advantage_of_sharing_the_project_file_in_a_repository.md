

FAQ What is the advantage of sharing the project file in a repository?
======================================================================

The .project file contains important information about your project, including what kind of project it is, what builders it contains, and what linked resources are attached to it. It is generally advisable to store this file in your code repository or to include the file if you zip the project up to give to a colleague. This will allow another user who is importing or loading the project to obtain this important information. If you don't, the project will appear in the other user's workspace as a simple project, with no builders or linked resources. Similarly, other plug-ins store important project metadata in the project content area, typically with a leading period on the file name. For example, the Java development tools store the project's build path in a file called .classpath. Plug-ins put their metadata in the project content area for a reason: to allow this information to be exported and loaded into other workspaces.

See Also:
---------

*   [FAQ How do I set up a Java project to share in a repository?](./FAQ_How_do_I_set_up_a_Java_project_to_share_in_a_repository.md "FAQ How do I set up a Java project to share in a repository?")

