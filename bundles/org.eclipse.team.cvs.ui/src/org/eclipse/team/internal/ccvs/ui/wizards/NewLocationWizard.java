/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;


import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.*;

public class NewLocationWizard extends Wizard implements INewWizard {
	
	protected ConfigurationWizardMainPage mainPage;
	protected Properties properties = null;
	private boolean switchPerspectives = true;
	
	/**
	 * Return the settings used for all location pages
	 */
	public static IDialogSettings getLocationDialogSettings() {
		IDialogSettings workbenchSettings = CVSUIPlugin.getPlugin().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("NewLocationWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("NewLocationWizard");//$NON-NLS-1$
		}
		return section;
	}
	
	public NewLocationWizard() {
		IDialogSettings section = getLocationDialogSettings();
		setDialogSettings(section);
		setWindowTitle(CVSUIMessages.NewLocationWizard_title);
		setNeedsProgressMonitor(true);
	}
	

	public NewLocationWizard(Properties initialProperties) {
		this();
		this.properties = initialProperties;
	}

	/**
	 * Creates the wizard pages
	 */
	@Override
	public void addPages() {
		mainPage = createMainPage();
		if (properties != null) {
			mainPage.setProperties(properties);
		}
		mainPage.setShowValidate(true);
		mainPage.setDescription(CVSUIMessages.NewLocationWizard_description); 
		mainPage.setDialogSettings(getDialogSettings());
		addPage(mainPage);
	}
	
	protected ConfigurationWizardMainPage createMainPage() {
		return new ConfigurationWizardMainPage("repositoryPage1", CVSUIMessages.NewLocationWizard_heading, CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_NEW_LOCATION)); //$NON-NLS-1$ 
	}
	
	@Override
	public boolean performFinish() {
		final ICVSRepositoryLocation[] location = new ICVSRepositoryLocation[] { null };
		boolean keepLocation = false;
		try {
			// Create a handle to a repository location
			location[0] = mainPage.getLocation();
			// Add the location quitely so we can validate
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
					keepLocation = true;
				} catch (InterruptedException e) {
					// Cancelled by user. Fall through to dispose of location
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
			} else {
				keepLocation = true;
			}
		} catch (TeamException e) {
			if (location[0] == null) {
				// Exception creating the root, we cannot continue
				CVSUIPlugin.openError(getContainer().getShell(), CVSUIMessages.NewLocationWizard_exception, null, e); 
				return false;
			} else {
				// Exception validating. We can continue if the user wishes.
				IStatus error = e.getStatus();
				if (error.isMultiStatus() && error.getChildren().length == 1) {
					error = error.getChildren()[0];
				}
					
				if (error.isMultiStatus()) {
					CVSUIPlugin.openError(getContainer().getShell(), CVSUIMessages.NewLocationWizard_validationFailedTitle, null, e); 
				} else {
					keepLocation = MessageDialog.openQuestion(getContainer().getShell(),
						CVSUIMessages.NewLocationWizard_validationFailedTitle, 
						NLS.bind(CVSUIMessages.NewLocationWizard_validationFailedText, (new Object[] {error.getMessage()}))); 
				}
			}
		}
		if (keepLocation) {
			KnownRepositories.getInstance().addRepository(location[0], true /* let the world know */);
			if (switchPerspectives) {
				final IWorkbench workbench= PlatformUI.getWorkbench();
				final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				
				final String defaultPerspectiveID= promptForPerspectiveSwitch();

				if (defaultPerspectiveID != null) {
					try {
						workbench.showPerspective(defaultPerspectiveID, window);
					} catch (WorkbenchException e) {
						Utils.handleError(window.getShell(), e, CVSUIMessages.ShowAnnotationOperation_0, e.getMessage()); 
					}
				}
			}
		} else {
			KnownRepositories.getInstance().disposeRepository(location[0]);
		}
		return keepLocation;	
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// Nothing to do
	}

	public void setSwitchPerspectives(boolean switchPerspectives) {
		this.switchPerspectives = switchPerspectives;
	}
	
	private String promptForPerspectiveSwitch() {
		// check whether we should ask the user.
		final IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
		final String option = store.getString(ICVSUIConstants.PREF_CHANGE_PERSPECTIVE_ON_NEW_REPOSITORY_LOCATION);
		final String desiredID = CVSPerspective.ID;
		
		if (option.equals(MessageDialogWithToggle.ALWAYS))
			return desiredID; // no, always switch
		
		if (option.equals(MessageDialogWithToggle.NEVER))
			return null; // no, never switch
		
		// Check whether the desired perspective is already active.
		final IPerspectiveRegistry registry= PlatformUI.getWorkbench().getPerspectiveRegistry();
		final IPerspectiveDescriptor desired = registry.findPerspectiveWithId(desiredID);
		final IWorkbenchPage page = CVSUIPlugin.getActivePage();
		
		if (page != null) {
			final IPerspectiveDescriptor current = page.getPerspective();
			if (current != null && current.getId().equals(desiredID)) {
				return null; // it is active, so no prompt and no switch
			}
		}
		
		if (desired != null) {
			
			String message;;
			String desc = desired.getDescription();
			if (desc == null) {
				message = NLS.bind(CVSUIMessages.NewLocationWizard_2, new String[] { desired.getLabel() });
			} else {
				message = NLS.bind(CVSUIMessages.NewLocationWizard_3, new String[] { desired.getLabel(), desc });
			}
			// Ask the user whether to switch
			final MessageDialogWithToggle m = MessageDialogWithToggle.openYesNoQuestion(
					Utils.getShell(null),
					CVSUIMessages.NewLocationWizard_1, 
					message, 
					CVSUIMessages.NewLocationWizard_4,   
					false /* toggle state */,
					store,
					ICVSUIConstants.PREF_CHANGE_PERSPECTIVE_ON_NEW_REPOSITORY_LOCATION);
			
			final int result = m.getReturnCode();
			switch (result) {
			// yes
			case IDialogConstants.YES_ID:
			case IDialogConstants.OK_ID :
				return desiredID;
			// no
			case IDialogConstants.NO_ID :
				return null;
			}
		}
		return null;
	}
}
