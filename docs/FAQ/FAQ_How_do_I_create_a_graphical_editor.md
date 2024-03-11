FAQ How do I create a graphical editor?
=======================================

The Eclipse Platform and SDK do not have support for creating graphical editors. However, a subproject of the Eclipse Tools Project is dedicated to a framework for graphical editors: the Graphical Editor Framework (GEF). GEF provides viewers for displaying graphical information and a controller for managing the interaction between those viewers and your domain model. GEF makes few assumptions about what your model looks like and imposes no restrictions on where its viewers can be displayed. This allows you to use GEF for displaying a wide variety of graphical information in dialogs, views, and editors within an Eclipse application.

See Also:
---------

The [GEF Web page](https://eclipse.org/gef)

