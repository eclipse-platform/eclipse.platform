

FAQ What is a wizard?
=====================

A wizard is a series of pages that guide a user through a complex task. **Back** and **Next** buttons allow the user to move forward and backward through the pages. Typically, each page collects a piece of information; when the user clicks the **Finish** button, the information is used to perform a task. At any time before clicking **Finish**, the user can cancel the task, which should undo any side effects of the steps completed so far.

![](https://github.com/eclipse-platform/eclipse.platform/tree/master/docs/FAQ/images/New_class_wizard.png)

New Class wizard

A wizard is typically presented in a dialog, but this is not required. The abstraction called IWizardContainer represents the context in which a wizard runs. A wizard container is guaranteed to have a title, a message area, and a progress monitor. A wizard must implement IWizard, and each page within the wizard must implement IWizardPage.

See Also:
---------

*   [FAQ How do I specify the order of pages in a wizard?](./FAQ_How_do_I_specify_the_order_of_pages_in_a_wizard.md "FAQ How do I specify the order of pages in a wizard?")
*   [FAQ How do I make my wizard appear in the UI?](./FAQ_How_do_I_make_my_wizard_appear_in_the_UI.md "FAQ How do I make my wizard appear in the UI?")

