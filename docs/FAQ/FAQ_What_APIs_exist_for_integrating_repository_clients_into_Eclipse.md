

FAQ What APIs exist for integrating repository clients into Eclipse?
====================================================================

Repositories, or version and configuration management (VCM) tools, are an essential part of software development. A wide variety of code repositories and a larger number of client-side tools for interacting with those repositories are available. These clients range from rudimentary command line tools to rich graphical clients that are deeply integrated with other development tools. As a tool-integration platform, repository client integration is an important aspect of Eclipse. From the very start, the core architecture of the Eclipse Platform has striven to allow deep integration of these repository client tools.

  
Early on, Eclipse used the term _VCM_ as a label for its repository integration components. Because acronyms in general aren't particularly descriptive and because this particular acronym didn't seem to be widely understood, the Eclipse Platform adopted the term _team tools_ as a replacement. This is why the repository APIs are found in the plug-ins org.eclipse.team.core and org.eclipse.team.ui and why the developers writing this stuff call themselves the _team_ team.

  
As with many Eclipse components, team tooling is divided into a generic, repository-agnostic layer and then separate layers for specific repository clients. The platform includes a reference implementation of the generic team APIs, which implements a powerful graphical client for integrating with _concurrent versions system_, or CVS. The team API centers on the notion of a _repository provider_. Each project in an Eclipse workspace can be associated with a single RepositoryProvider subclass that acts as the entry point for repository interaction. The RepositoryProvider API and the remainder of the team-integration APIs are extremely well documented, so we don't need to go into more detail here.

The _Platform Plug-in Developer Guide_ includes detailed documentation on how to implement your own repository provider. See the section **Programmer's guide >** Team support**.**

  

See Also:
---------

[FAQ\_What\_support\_does\_the\_workspace\_have\_for\_team_tools?](./FAQ_What_support_does_the_workspace_have_for_team_tools.md "FAQ What support does the workspace have for team tools?")

