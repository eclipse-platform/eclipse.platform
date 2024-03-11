

FAQ Can I reuse wizards from other plug-ins?
============================================

Yes, as long as the wizards expose them as API. Most of the time, plug-ins will make their wizard pages API but will not make the wizard itself API. The reason is that most of the interesting functionality is in the pages anyway, and other plug-ins wanting to reuse a wizard will generally want to insert some of their own pages as well.

Almost all the wizard pages you see in the Eclipse SDK are available as API, so you can use them in your own wizard. The pages from the import and export wizards, along with the wizards for creating simple files, folders, and projects, are found in the org.eclipse.ui.ide plug-in in the org.eclipse.ui.dialogs package. The import and export pages are abstract, but they allow subclasses to insert the controls for the source or destination of the import, and the abstract page takes care of the rest. The org.eclipse.jdt.ui plug-in exposes wizard pages for creating Java projects, packages, and types in the org.eclipse.jdt.ui.wizards package.

See Also:
---------

*   [FAQ How can I reuse wizard pages in more than one wizard?](./FAQ_How_can_I_reuse_wizard_pages_in_more_than_one_wizard.md "FAQ How can I reuse wizard pages in more than one wizard?")

