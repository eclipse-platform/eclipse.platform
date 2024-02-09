

FAQ How do I open a Property dialog?
====================================

The Property dialog appears when you select an object in a view and choose **Properties...** from the context menu or **File** menu. If you want such an action for your own view or editor, you can simply add an instance of org.eclipse.ui.dialogs.PropertyDialogAction to your view's context menu. This action will compute what property pages are applicable for the current selection and then open a dialog on those property pages.

  
You can also open a Property dialog on a set of property pages of your own choosing. This works exactly like the JFace Preference dialog: You supply the dialog with a PreferenceManager instance that knows how to build the set of pages to be shown. The only difference with the Property dialog is that you must also supply it with the current selection:

      ISelection sel = ... obtain the current selection
      PropertyPage page = new MyPropertyPage();
      PreferenceManager mgr = new PreferenceManager();
      IPreferenceNode node = new PreferenceNode("1", page);
      mgr.addToRoot(node);
      PropertyDialog dialog = new PropertyDialog(shell, mgr, sel);
      dialog.create();
      dialog.setMessage(page.getTitle());
      dialog.open();

See Also:
---------

[FAQ\_How\_do\_I\_launch\_the\_preference\_page\_that\_belongs\_to\_my\_plug-in?](./FAQ_How_do_I_launch_the_preference_page_that_belongs_to_my_plug-in.md "FAQ How do I launch the preference page that belongs to my plug-in?")

[FAQ\_How\_do\_I\_find\_out\_what\_object\_is_selected?](./FAQ_How_do_I_find_out_what_object_is_selected.md "FAQ How do I find out what object is selected?")

