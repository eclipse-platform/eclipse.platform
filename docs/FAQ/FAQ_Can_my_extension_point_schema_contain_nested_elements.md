

FAQ Can my extension point schema contain nested elements?
==========================================================

Yes. The extension point schema supports top-level elements with attributes. Each attribute can refer to another element in the schema. The Schema Editor has a difficult job indicating this, but the nesting can be observed in Figure 4.4 that shows an example that uses the org.eclipse.ui.actionSets extension point. The schema for this extension point explicitly defines the grammar rules for nesting one or more menus and actions into one action set (Figure 4.5). A similar hierarchy can be used for your own extension point schemas.

See Also:
---------

*   [FAQ What is an extension point schema?](./FAQ_What_is_an_extension_point_schema.md "FAQ What is an extension point schema?")

