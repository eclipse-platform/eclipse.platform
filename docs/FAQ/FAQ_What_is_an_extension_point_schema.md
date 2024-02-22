

FAQ What is an extension point schema?
======================================

Each extension point has a _schema_ file that declares the elements and attributes that extensions to that point must declare. The schema is used during plug-in development to detect invalid extensions in the plugin.xml files in your workspace and is used by the schema-based extension wizard in the plug-in Manifest Editor to help guide you through the steps to creating an extension. Perhaps most important, the schema is used to store and generate documentation for your extension point. The schema is _not_ used to perform any runtime validation checks on plug-ins that connect to that extension point. In fact, extension point schema files don't even need to exist in a deployed plug-in.

The exact format of the schema file is an implementation detail that you probably don't want to become familiar with. Instead, you should use the graphical schema editor provided by the Plug-in Development Environment.

See Also:
---------

*   [FAQ How do I declare my own extension point?](./FAQ_How_do_I_declare_my_own_extension_point.md "FAQ How do I declare my own extension point?")
*   [FAQ Can my extension point schema contain nested elements?](./FAQ_Can_my_extension_point_schema_contain_nested_elements.md "FAQ Can my extension point schema contain nested elements?")
*   [FAQ What are extensions and extension points?](./FAQ_What_are_extensions_and_extension_points.md "FAQ What are extensions and extension points?")

