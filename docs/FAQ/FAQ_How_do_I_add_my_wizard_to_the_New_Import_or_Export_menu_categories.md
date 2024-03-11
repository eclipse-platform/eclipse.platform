

FAQ How do I add my wizard to the New, Import, or Export menu categories?
=========================================================================

Some special kinds of wizards have to be registered with the platform in your plugin.xml file. These wizards are found under the **File > New**, **File > Import**, and **File > Export** menu actions. These wizards are declared using the org.eclipse.ui newWizards, importWizards, and exportWizards extension points, respectively. Once you have declared your wizard with the appropriate extension point, the platform will take care of displaying it in the appropriate places. Following is an example declaration of a new wizard:

      <extension
            point="org.eclipse.ui.newWizards">
            <wizard
                  name="New Addition"
                  class="org.eclipse.faq.examples.AddingWizard"
                  id="org.eclipse.faq.examples.addingWizard">
            </wizard>
      </extension>

This wizard will appear by default under **File > New > Other...**. To make the wizard appear under the new-project category, add the attribute project="true" to the extension declaration.

To add your own wizard category, use

      <category 
          id="org.eclipse.faq.examples.MyWizard" 
          name="FAQ Wizards">
      </category> 
      <category 
          id="org.eclipse.faq.examples.WizardSubCategory" 
          name="More Specific FAQ Wizards">
          parentCategory="org.eclipse.faq.examples.MyWizard" 
      </category> 

See Also:
---------

*   [FAQ How do I make my wizard appear in the UI?](./FAQ_How_do_I_make_my_wizard_appear_in_the_UI.md "FAQ How do I make my wizard appear in the UI?")

