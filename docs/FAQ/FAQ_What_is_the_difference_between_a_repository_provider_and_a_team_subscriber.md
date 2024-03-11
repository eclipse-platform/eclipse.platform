

FAQ What is the difference between a repository provider and a team subscriber?
===============================================================================

  

  

  
The Eclipse Platform team component supports two categories of tool integration. Of primary interest are _repository providers_, which represent full-fledged versioning and configuration management tools, such as CVS, ClearCase, and Subversion. These tools typically include support for maintaining an arbitrary number of versions of files and file trees, for branching and merging development streams, and for linking versions to bug tracking and other configuration management tools. Clients for interacting with these tools are represented in Eclipse through the RepositoryProvider API.

  
Another class of team tooling is used for deployment of development artifacts to a remote execution target. For example, you may use FTP or WebDAV to deploy code and other resources to a Web server, or a proprietary protocol for deploying code to embedded and hand-held devices. This class of tooling is represented by the _team subscriber_ API.

Note the extensive overlap between these two categories of team tooling. In general, team subscribers represent a subset of the functionality provided by a repository client. In other words, if you're writing a client for a repository, you will most likely need both the repository provider and the team subscriber API.

  

See Also:
---------

[FAQ\_What\_APIs\_exist\_for\_integrating\_repository\_clients\_into_Eclipse?](./FAQ_What_APIs_exist_for_integrating_repository_clients_into_Eclipse.md "FAQ What APIs exist for integrating repository clients into Eclipse?")

[FAQ\_How\_do\_I\_deploy\_projects\_to\_a\_server\_and\_keep\_the\_two_synchronized?](./FAQ_How_do_I_deploy_projects_to_a_server_and_keep_the_two_synchronized.md "FAQ How do I deploy projects to a server and keep the two synchronized?")

