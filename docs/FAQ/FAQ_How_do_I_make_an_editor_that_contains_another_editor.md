

FAQ How do I make an editor that contains another editor?
=========================================================

In its most abstract sense, an editor is simply a container for arbitrary SWT controls. As such, an editor can contain views, editors, wizards, and any other visual control. In practice, many of these types of controls don't make sense in the context of an editor. However, an editor is commonly composed of several pages, some of which may also be used as stand-alone editors. The most common example is an editor that provides a rendered WYSIWYG (what you see is what you get) page and a source page that shows raw text. The PDE plug-in Manifest Editor, implemented by the ManifestEditor class in the PDE UI plug-in, is such an editor. The platform provides infrastructure for this common editor type in the MultiPageEditorPart class.

Creating a subclass of MultiPageEditorPart requires that you implement many of the same methods as you would for a standard editor. The main difference is that instead of implementing createPartControl, you must implement createPages. In this method, you must create the controls for each page within the editor. These pages can be either standard editor parts or arbitrary SWT controls. The nested editors don't need to know anything about their container; from their point of view they are simply standard editors.

Your subclass of MultiPageEditorPart isn't required to do much beyond creating the initial set of pages. The superclass will take care of implementing the presentation for displaying the pages and for allowing the user to switch pages. If necessary, you can programmatically add or remove pages at any time by calling addPage or removePage. You can also programmatically switch pages by using setActivePage and respond to page changes by overriding pageChange.

The simplest way to get going with a multi-page editor is to edit your plugin.xml with the Manifest Editor. Select the **Extensions** tab, click on **Add...**, select **org.eclipse.ui.editors** in the extension point list, and choose **Multi-page Editor** from the list of available templates. This will add all the required pieces for a basic multi-page editor to your plug-in.

See Also:
---------

*   [FAQ How do I create a form-based editor, such as the plug-in Manifest Editor?](./FAQ_How_do_I_create_a_form-based_editor_such_as_the_plug-in_Manifest_Editor.md "FAQ How do I create a form-based editor, such as the plug-in Manifest Editor?")

