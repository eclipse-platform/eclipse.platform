

FAQ How do I create my own view?
================================

The simplest way is to use the PDE wizard to create a view example and go from there. Select **File > New > Other... > Plug-in Development** \> Plug-in Project**, and choose a name and ID for your** plug-in. Then, in the plug-in code generator wizard page, choose **Plug-in with a view**. Look at the generated plugin.xml file for the generated extension point code.

To add a view to an existing plug-in, edit your plugin.xml with the Manifest Editor, choose the **Extensions** tab, click **Add...**, select **Extension Templates**, and choose **Sample View**.

To see how other plug-ins use views, search on the org.eclipse.ui.views extension point in the Search dialog. Choose the **Plug-in Search** tab, and enter the name of the extension point.

