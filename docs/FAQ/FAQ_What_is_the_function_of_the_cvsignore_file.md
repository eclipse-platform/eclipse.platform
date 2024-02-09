

FAQ What is the function of the .cvsignore file?
================================================

To exclude resources from management under CVS version control, a .cvsignore file can be placed in the parent folder of a resource. Each folder has to specify its own .cvsignore file as there is no logic for path-relative resource specification.

The Navigator has UI support for CVS exclusion in its context menu for any resource in a project. Follow **Team > Add to .cvsignore** to the dialog that allows selection based on the name of the resource, a wildcard, or a custom pattern to match multiple resources.

The .cvsignore file is not an Eclipse invention. Its syntax is defined by CVS, and the Eclipse team plug-ins simply pass the file on to CVS when sharing a project.

See Also:
---------

*   [The CVS manual](http://www.cvshome.org)


