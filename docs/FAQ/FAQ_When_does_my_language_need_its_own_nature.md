

FAQ Why should I add my own project nature?
===========================================

(Redirected from [FAQ When does my language need its own nature?](/index.php?title=FAQ_When_does_my_language_need_its_own_nature%3F&redirect=no "FAQ When does my language need its own nature?"))

Project natures act as tags on a project to indicate that a certain tool is used to operate on that project

Why shouldn't add my own project nature?
----------------------------------------

As natures are an Eclipse-specific concept and have some limitations, it's sometimes difficult for users to understand and manipulate them. It's recommended to do as much as possible without using a project nature. In many case, checking for a file content-type or the existence of some specific file in a project can be enough to trigger some features or builders and configure them, without a new nature.

Also, at the moment, [user cannot add/remove nature](https://bugs.eclipse.org/bugs/show_bug.cgi?id=102527) from projects on vanilla Eclipse IDE. If you think user is likely to with to enable/disable your nature, this can be a no-go.

Adding a project nature
-----------------------

Project natures act as tags on a project to indicate that a certain tool is used to operate on that project. They can also be used to distinguish projects that your plug-in is interested in from the rest of the projects in the workspace. For example, natures can be used to filter declarative extensions that operate only on projects of a particular type. The propertyPages and popupMenus extension points allow you to filter enablement of an extension, based on various properties of the selected resource. One of the properties that this mechanism understands is the nature of a project. Here is an example of an actionSet declaration that operates only on files in projects with the PDE nature:

      <extension point="org.eclipse.ui.popupMenus">
         <objectContribution
            objectClass="org.eclipse.core.resources.IFile"
            id="org.eclipse.pde.ui.featureToolSet">
            <filter
               name="projectNature"
               value="org.eclipse.pde.FeatureNature">
            </filter>
            ...

Another reason for using natures is to make use of the nature lifecycle methods when your plug-in is connected to or disconnected from a project. When a nature is added to a project for the first time, the nature's configure method is called. When the nature is removed from the project, the deconfigure method is called. This is an opportunity to initialize metadata on the project and to associate additional attributes, such as builders, with the project.

See Also:
---------

*   Go to **Platform Plug-in Developer Guide > Programmer's Guide > Advanced Resources concepts > Project natures**
*   [Eclipse online article Project Natures and Builders](https://www.eclipse.org/articles/Article-Builders/builders.html)
    *   This article shows how to bind a project nature to a builder.
*   [FAQ When does my language need its own perspective?](./FAQ_When_does_my_language_need_its_own_perspective.md "FAQ When does my language need its own perspective?")

