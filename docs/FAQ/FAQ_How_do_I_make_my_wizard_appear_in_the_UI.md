

FAQ How do I make my wizard appear in the UI?
=============================================

To make a wizard appear, you need an implementation of the wizard interface called IWizardContainer. The container is responsible for all the presentation outside the pages themselves, including a title area, button bar, and progress indicator. You can implement this interface yourself if you want to embed a wizard into a custom control. JFace provides a default wizard container that is a simple modal dialog: WizardDialog. The following code snippet opens a wizard in a wizard dialog:

      Shell shell = window.getShell();
      AddingWizard wizard = new AddingWizard();
      WizardDialog dialog = new WizardDialog(shell, wizard);
      int result = dialog.open();

See Also:
---------

*   [FAQ What is a wizard?](./FAQ_What_is_a_wizard.md "FAQ What is a wizard?")
*   [FAQ How do I add my wizard to the New, Import, or Export menu categories?](./FAQ_How_do_I_add_my_wizard_to_the_New_Import_or_Export_menu_categories.md "FAQ How do I add my wizard to the New, Import, or Export menu categories?")

