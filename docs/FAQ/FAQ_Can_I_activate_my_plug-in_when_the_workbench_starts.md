FAQ Can I activate my plug-in when the workbench starts?
========================================================

You shouldn't.
The principle of lazy plug-in activation is very important in a platform with an open-ended set of plug-ins. 
All plug-in writers like to think that their plug-in are important enough to contravene this rule, but when an Eclipse install can contain thousands of plug-ins, you have to keep the big picture in mind. 
Having said that, you can use a mechanism to activate your plug-in when the workbench starts. 

You can use:

* OSGi immediate components
* OSGi services

If you use these methods consider using OSGi declarative services.

See Also:
---------

*   [FAQ When does a plug-in get started?](./FAQ_When_does_a_plug-in_get_started.md "FAQ When does a plug-in get started?")
*   [OSGi Tutorial](https://www.vogella.com/tutorials/OSGi/article.html)
