

FAQ How do I create a plug-in?
==============================

The simplest way to develop and maintain a plug-in is to use the special wizards and editors the PDE provides. Using a wizard, you can generate basic plug-ins with a few mouse clicks. After some practice, you can create an extension to Eclipse, and even launch it in a runtime workbench, in under a minute. A lot of XML and Java code is automatically generated for you.

Select **File > New > Other... > Plug-in Development > Plug-in Project > Next** and choose a suitable name for your plug-in. Most Eclipse plug-in names start with org.eclipse, but you can choose any suitable name here. Accept all the defaults following in the next wizard pages by clicking **Next** until you get to the wizard page shown in Figure 4.1.

Using one of the provided code templates will get you going quickly. Once it is generated, you can edit the generated plug-in code by using the special PDE Manifest Editor that has various editors with both visual and textual views of the underlying XML.

Using the combination of PDE code wizards and editors, you can quickly generate and maintain plug-in code. Once you get more experienced, you will

learn how to travel down into the XML code that describes your plug-in and directly influence the behavior for your plug-in to do more advanced tasks.
 
We advise using wizards as much as possible to develop or maintain your plug-ins. However, at the same time, wizards will take you only so far. At some point, you have to dive in and modify or enhance the generated code to make the plug-ins fit your own needs. Luckily, the PDE allows you to smoothly transition between wizards, graphical editors, and full-text editors of plugin.xml files. Changes in one view are reflected in the other, and plug-in authors can jump back and forth between textual and visual editing forms.

We also recommend checking out the new Cheat Sheet support added in Eclipse 3.0, see **Help > Cheat Sheets...**, for an excellent step-by-step- guide to developing plug-ins.

See Also:
---------

[FAQ\_How\_do\_I\_use\_the\_plug-in\_Manifest\_Editor?](./FAQ_How_do_I_use_the_plug-in_Manifest_Editor.md "FAQ How do I use the plug-in Manifest Editor?")

  
[FAQ\_What\_is\_a\_plug-in?](./FAQ_What_is_a_plug-in.md "FAQ What is a plug-in?")

