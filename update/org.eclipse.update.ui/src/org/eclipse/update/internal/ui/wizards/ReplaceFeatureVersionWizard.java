package org.eclipse.update.internal.ui.wizards;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.UpdateUIImages;


public class ReplaceFeatureVersionWizard extends Wizard {
	private ReplaceFeatureVersionWizardPage page;

	public ReplaceFeatureVersionWizard(IFeature currentFeature, IFeature[] features) {
		setWindowTitle(UpdateUI.getString("SwapFeatureWizard.title")); //$NON-NLS-1$
		setDefaultPageImageDescriptor(UpdateUIImages.DESC_UPDATE_WIZ);
		page = new ReplaceFeatureVersionWizardPage(currentFeature, features);
	}

	public void addPages() {
		addPage(page);
	}
	
	public boolean performFinish() {
		return page.performFinish();
	}

}
