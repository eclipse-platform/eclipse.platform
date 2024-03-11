

FAQ What is the difference between a product and an application?
================================================================

At first glance, the notions of _product_ and _application_ in Eclipse seem similar. Both are contributed via an extension point, and both are used to bring order to the chaos of an otherwise random collection of executing Eclipse plug-ins. However, the two concepts have some important differences.

First, an application defines behavior, so it has code associated with it. A product on the other hand is purely declarative. That is, a product provides properties, such as icons and text, that are used to customize the appearance of a running application.

A second distinction is that there is typically only one product, but that product may include several applications. For example, the Eclipse Platform is a single product but includes several applications, such as the workbench application and an application for running custom Ant build files. All applications under a given product have the same branding elements associated with them.

  

See Also:
---------

[FAQ\_What\_is\_an\_Eclipse_application?](./FAQ_What_is_an_Eclipse_application.md "FAQ What is an Eclipse application?")

[FAQ\_What\_is\_an\_Eclipse_product?](./FAQ_What_is_an_Eclipse_product.md "FAQ What is an Eclipse product?")

