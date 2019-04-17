/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.*;

public class AlternativeLocationWizard extends NewLocationWizard {

	private ICVSRepositoryLocation location;
	
	@Override
	public boolean performFinish() {
		final ICVSRepositoryLocation[] location = new ICVSRepositoryLocation[] { null };
		boolean useLocation = true;
		try {
			// Create a handle to a repository location
			location[0] = mainPage.getLocation();
			// Add the location quietly so we can validate
			location[0] = KnownRepositories.getInstance().addRepository(location[0], false /* don't tell anybody */);
			
			if (mainPage.getValidate()) {
				try {
					getContainer().run(true, true, monitor -> {
						try {
							location[0].validateConnection(monitor);
						} catch (TeamException e) {
							throw new InvocationTargetException(e);
						}
					});
				} catch (InterruptedException e) {
					// Canceled by user. Fall through to dispose of location
				} catch (InvocationTargetException e) {
					Throwable t = e.getTargetException();
					if (t instanceof TeamException) {
						throw (TeamException)t;
					} else if (t instanceof Exception) {
						throw CVSException.wrapException((Exception)t);
					} else {
						throw CVSException.wrapException(e);
					}
				}
			} 
		} catch (TeamException e) {
			if (location[0] == null) {
				// Exception creating the root, we cannot continue
				CVSUIPlugin.openError(getContainer().getShell(), CVSUIMessages.AlternativeLocationWizard_exception, null, e); 
				return false;
			} else {
				// Exception validating. We can continue if the user wishes.
				IStatus error = e.getStatus();
				if (error.isMultiStatus() && error.getChildren().length == 1) {
					error = error.getChildren()[0];
				}
					
				if (error.isMultiStatus()) {
					CVSUIPlugin.openError(getContainer().getShell(), CVSUIMessages.AlternativeLocationWizard_validationFailedTitle, null, e); 
				} else {
					useLocation = MessageDialog.openQuestion(getContainer().getShell(),
						CVSUIMessages.AlternativeLocationWizard_validationFailedTitle, 
						NLS.bind(CVSUIMessages.AlternativeLocationWizard_validationFailedText, (new Object[] {error.getMessage()}))); 
				}
			}
		}
		
		if (useLocation) {
			KnownRepositories.getInstance().addRepository(location[0], true /* let the world know */);
		} else {
			KnownRepositories.getInstance().disposeRepository(location[0]);
		}
		
		this.location = useLocation ? location[0] : null;
		return useLocation;
	}
	
	public AlternativeLocationWizard(Properties initialProperties) {
		super(initialProperties);
	}
	
	/**
	 * Creates the wizard pages
	 */
	@Override
	public void addPages() {
		super.addPages();
	}
	
	@Override
	protected ConfigurationWizardMainPage createMainPage() {
		return new AlternativeConfigurationWizardMainPage("repositoryPage1", CVSUIMessages.AlternativeLocationWizard_heading, CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_NEW_LOCATION)); //$NON-NLS-1$
	}
	
	public ICVSRepositoryLocation getLocation() {
		return location;
	}
	
	/**
	 * Wizard page for entering information about a CVS repository location used
	 * while working with Alternative Repository dialog.
	 * 
	 * <p>
	 * Validation works slightly different. When user wants to create a
	 * location, which already exists he/she will be informed that the location
	 * can be obtained from a combo-box.
	 * </p>
	 * 
	 * TODO: enable to create existing location, instead of displaying an error
	 * close the dialog and select the entry with that location in the combo box
	 */
	private class AlternativeConfigurationWizardMainPage extends
			ConfigurationWizardMainPage {

		public AlternativeConfigurationWizardMainPage(String pageName,
				String title, ImageDescriptor titleImage) {
			super(pageName, title, titleImage);
		}

		@Override
		protected void validateFields() {
			super.validateFields();
			if (!isPageComplete()
					&& getErrorMessage() != null
					&& getErrorMessage().equals(
							CVSUIMessages.ConfigurationWizardMainPage_0)) {
				// add an information that the location already exists and can
				// be selected from a combo box
				setErrorMessage(CVSUIMessages.AlternativeConfigurationWizardMainPage_0);
			}
		}
	}
	
}
