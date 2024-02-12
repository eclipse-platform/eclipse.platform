

FAQ How do I ask a simple yes or no question?
=============================================

The MessageDialog class provides a couple of convenience methods for asking questions with Boolean answers. The openQuestion method will present a dialog with Yes and No buttons and will return the result. The openConfirm method is similar but uses Ok and Cancel as the button labels. If you want to use different button labels or have more buttons, such as Yes/No/Always/Never, you can construct a customized dialog yourself:

      MessageDialog dialog = new MessageDialog(
         null, "Title", null, "Question",
         MessageDialog.QUESTION,
         new String[] {"Yes", "No", "Always", "Never"},
         0);, // yes is the default
      int result = dialog.open();

The return value from the open method will be the index of the button in the label array. If the user cancels or closes the dialog, a result of one is returned by convention. This means that you should try to make your second button match the behavior that makes sense for your circumstances. If you want completely different behavior for dialog cancellation, you will need to subclass MessageDialog and override the cancelPressed method.

  

  
The MessageDialogWithToggle class-introduced in Eclipse 3.0-is an extension of MessageDialog that adds the capability of remembering the user's selection to avoid having to prompt them again. This can be used for introductory warning messages that advanced users want to be able to turn off. MessageDialogWithToggle has similar static convenience methods as MessageDialog for common questions, but you can use the constructor to provide a customized set of buttons if necessary.

