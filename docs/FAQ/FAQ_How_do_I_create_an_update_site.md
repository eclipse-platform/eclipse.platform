

FAQ How do I create an update site (site.xml)?
==============================================

An update site may contain your own plugins, as well as public plugins mirrored.

create an own plugin
--------------------

First create a new plugin project

New -> Plug-in Development -> Plug-in Project

![Plugin.jpg](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/master/docs/FAQ/images/Plugin.jpg)

You can select a template for creating your project or even just create a blank one in case you know how things work.

Now you have to create your feature project.

New -> Plug-in Development -> Feature Project

![Feature.jpg](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/master/docs/FAQ/images/Feature.jpg)

Here is the point where you link your Plugin project with your Feature Project.

Open the feature.xml file on your Feature project.

On plugins tab click on add and select your project then save the file.

![Feature-plugin.jpg](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/master/docs/FAQ/images/Feature-plugin.jpg)

The last piece is the Update site project. See below

create an update site out of installed plugins
----------------------------------------------

To create it go to

New -> Plug-in Development -> Update Site Project

![Site.jpg](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/master/docs/FAQ/images/Site.jpg)

Now we just have to link the Feature project with the Update site, and you're good to go.

To do that, open the site.xml file on your Site project.

Add Feature -> Select your project.

![Site-feature.jpg](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/master/docs/FAQ/images/Site-feature.jpg)

In case you want to add a category for your feature, just click on New Category,

name it as you want then drag the feature site over the category.

The category is the name displayed when a eclipse client is installing your plugin by update-site.

**Save** it then click on **Build All** to build all features and plug-ins recursively required for this update site.

You can simply drag the contents of your Update Site project to an file transfer protocol (FTP) client to publish at a Web site.

Alternatively, you can even test out the update site directly by selecting **Help > Install New Software**, then **Add > Local...**, find your update site in the file system, then accept all changes by pressing **OK**, select your feature and proceed with the dialog (**Next**).

  

  

