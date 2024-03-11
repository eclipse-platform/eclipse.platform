

FAQ Can I install plug-ins outside the main install directory?
==============================================================

Users who like to live on the edge will be frequently installing new builds of Eclipse. When a new build is installed, a manual step is generally required to copy over any extra plug-ins from the old build to the new one. We all know how much programmers hate manual steps, so it would be nice if there were an easy way to link a set of external plug-ins into Eclipse builds. A mechanism to do that is called a _product extension_.

A product extension must be laid out on disk in a certain way so the Eclipse configuration tools can recognize it. Following is the disk layout for a product extension that contains a single plug-in called org.eclipse.faq.examples.

      eclipse/
         .eclipseextension
         plugins/
            org.eclipse.faq.examples/
               plugin.xml
               examples.jar
            ... optionally more plug-in directories ...
         features/
            ... features would go here ...

The file .eclipseextension is empty, acting as a special marker that tells install tools that this is an Eclipse extension. Other than that special file, the layout is the same as that for an Eclipse product. Plug-ins go in a directory called plugins, and if the extension contains features, they go in a sibling directory called features.

Once you've got this directory structure set up, you have to link the product extension into your plug-in configuration. In Eclipse 3.0, you simply go to **Help > Software Updates > Manage Configuration**, choose the option called **Add an Extension Location**, and select the extension directory when prompted. That's all there is to it! As long as you keep the same workspace when you upgrade to a new build, the product extensions will automatically be available in the new configuration.

  

See Also:
---------

[FAQ\_How\_do\_I\_upgrade_Eclipse?](./FAQ_How_do_I_upgrade_Eclipse.md "FAQ How do I upgrade Eclipse?")

[FAQ\_How\_do\_I\_remove\_a\_plug-in?](./FAQ_How_do_I_remove_a_plug-in.md "FAQ How do I remove a plug-in?")

