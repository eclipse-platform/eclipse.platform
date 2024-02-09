

FAQ How do I create a form-based editor, such as the plug-in Manifest Editor?
=============================================================================

The org.eclipse.ui.forms plug-in provides a framework for building form-based editors. The components in this plug-in have long been the framework for building the PDE and Install/Update editors and views but have been made into official API only in the Eclipse 3.0 release.

To give you a quick overview, a form-based editor is created by subclassing the abstract FormEditor class. This class allows you to add any number of tabbed pages, which can be either traditional editor components or form-based pages (IFormPage). Each form page creates the displays for a single IForm, and each form may contain multiple FormParts representing each section in the form. There are various flavors of IForm subtypes, depending on whether you want multiple sections, scrolling, or different layout styles. An example of a form-based editor is the PDE plug-in Manifest Editor, implemented by the ManifestEditor class in the org.eclipse.pde.ui plug-in.

