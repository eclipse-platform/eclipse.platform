

FAQ Can I activate my plug-in when the workbench starts?
========================================================

You shouldn't. The principle of lazy plug-in activation is very important in a platform with an open-ended set of plug-ins. All plug-in writers like to think that their plug-in are important enough to contravene this rule, but when an Eclipse install can contain thousands of plug-ins, you have to keep the big picture in mind. Having said that, you can use a mechanism to activate your plug-in when the workbench starts. In special circumstances or for hobby plug-ins that aren't meant for wider use, you can use the org.eclipse.ui.startup extension point to activate your plug-in as soon as the workbench starts up.

The startup extension point allows you to specify a class that implements the IStartup interface. If you omit the class attribute from the extension, your Plugin subclass will be used and therefore must implement IStartup. This class will be loaded in a background thread after the workbench starts, and its earlyStartup method will be run. As always, however, your Plugin class will be loaded first, and its startup method will be called before any other classes are loaded. The earlyStartup method essentially lets you distinguish eager activation from normal plug-in activation.

Note that even when this extension point is used, the user can always veto the eager activation from the **Workbench > Startup** preference page. This illustrates the general Eclipse principle that the user is always the final arbiter when conflicting demands on the platform are made. This also means that you can't rely on eager activation in a production environment. You will always need a fallback strategy when the user decides that your plug-in isn't as important as you thought it was.

See Also:
---------

*   [FAQ When does a plug-in get started?](./FAQ_When_does_a_plug-in_get_started.md "FAQ When does a plug-in get started?")

