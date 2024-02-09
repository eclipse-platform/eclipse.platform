

FAQ How do I add other plug-ins' actions to my menus?
=====================================================

Many plug-ins expose their actions as API, allowing you to instantiate them and add them to your menus. The trend in Eclipse 3.0 is to introduce factory methods for creating actions, allowing the actual action implementation to remain hidden. See ActionFactory and IDEActionFactory for examples of such action factories.

  
Views and editors can register their context menus with the platform to allow other plug-ins to add actions to them dynamically. However, most actions in the platform are not contributed dynamically. It would create a lot of clutter if everyone added actions to everyone else's views.

  

See Also:
---------

\[\[FAQ\_Can\_other\_plug-ins\_add\_actions\_to\_my\_part%26%23146%3Bs\_context\_menu%3F\]\]

