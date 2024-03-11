

FAQ How do I specify the order of pages in a wizard?
====================================================

In its simplest and most common form, a wizard is made up of a static, ordered list of pages. A wizard adds pages to the list by using the Wizard.addPage method, and the **Next** and **Back** buttons simply cycle through this ordered list. For a wizard with a static set of pages, you can simply override the addPages method and add all of your pages at once. This snippet is from AddingWizard in the FAQ Examples plug-in:

      public class AddingWizard extends Wizard {
         private AddingWizardPage page1, page2;
         ...
         public void addPages() {
            page1 = new AddingWizardPage("Page1", 
               "Please enter the first number");
            addPage(page1);
            page2 = new AddingWizardPage("Page2", 
               "Enter another number");
            addPage(page2);
         }
      }

If you want to change the behavior of the wizard, depending on the values that are entered, you can dynamically create new pages or change the page order after the wizard is opened. Pages must still be added to the wizard by using the addPage method, but the progression of pages through the wizard can change, depending on the user's input. This ordering is changed by overriding the wizard methods getNextPage and getPreviousPage, which are called when the user clicks on **Next** or **Back** button.

Even with dynamic wizards, you must create at least one page before the wizard is opened. If only one page is added at the beginning, but more pages will be added later on, you will need to call Wizard.setForcePreviousAndNextButtons(true) before the wizard is opened. Otherwise, the **Next** and **Back** buttons will appear only if more than one page is in the page list.

See Also:
---------

*   [FAQ What is a wizard?](./FAQ_What_is_a_wizard.md "FAQ What is a wizard?")

