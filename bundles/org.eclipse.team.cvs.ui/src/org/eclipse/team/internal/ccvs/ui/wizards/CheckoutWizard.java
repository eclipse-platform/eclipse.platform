/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
 *     Philippe Ombredanne - bug 84808
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.operations.CheckoutMultipleProjectsOperation;
import org.eclipse.team.internal.ccvs.ui.operations.ProjectMetaFileOperation;
import org.eclipse.ui.*;

/**
 * Gathers all information necessary for a checkout from a repository.
 */
public class CheckoutWizard extends Wizard implements ICVSWizard, INewWizard {
	
	private RepositorySelectionPage locationPage;
	private ConfigurationWizardMainPage createLocationPage;
	private ModuleSelectionPage modulePage;
	private CheckoutAsWizard wizard;
	private ICVSRepositoryLocation location;
	private boolean isNewLocation;
	private CVSWizardPage dummyPage;
	
	public CheckoutWizard() {
		setWindowTitle(CVSUIMessages.CheckoutWizard_0); 
	}
	
	@Override
	public void addPages() {
		setNeedsProgressMonitor(true);
		
		ImageDescriptor substImage = getBannerImageDescriptor();

		ICVSRepositoryLocation[] locations = CVSUIPlugin.getPlugin().getRepositoryManager().getKnownRepositoryLocations();
		if (locations.length > 0) {
			locationPage = new RepositorySelectionPage("locationSelection", CVSUIMessages.CheckoutWizard_7, substImage); //$NON-NLS-1$ 
			locationPage.setDescription(CVSUIMessages.SharingWizard_importTitleDescription); 
			locationPage.setExtendedDescription(CVSUIMessages.CheckoutWizard_8); 
			addPage(locationPage);
		}
		
		createLocationPage = new ConfigurationWizardMainPage("createLocationPage", CVSUIMessages.SharingWizard_enterInformation, substImage); //$NON-NLS-1$ 
		createLocationPage.setDescription(CVSUIMessages.SharingWizard_enterInformationDescription); 
		addPage(createLocationPage);
		createLocationPage.setDialogSettings(NewLocationWizard.getLocationDialogSettings());
		
		modulePage = new ModuleSelectionPage("moduleSelection", CVSUIMessages.CheckoutWizard_10, substImage); //$NON-NLS-1$ 
		modulePage.setDescription(CVSUIMessages.CheckoutWizard_11); 
		modulePage.setHelpContxtId(IHelpContextIds.CHECKOUT_MODULE_SELECTION_PAGE);
		modulePage.setSupportsMultiSelection(true);
		addPage(modulePage);
		
		// Dummy page to allow lazy creation of CheckoutAsWizard
		dummyPage = new CVSWizardPage("dummyPage") { //$NON-NLS-1$
			@Override
			public void createControl(Composite parent) {
				Composite composite = createComposite(parent, 1, false);
				setControl(composite);
			}
		};
		addPage(dummyPage);
	}

	protected ImageDescriptor getBannerImageDescriptor() {
		return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_CHECKOUT);
	}
	
	@Override
	public boolean canFinish() {
		return (wizard == null && getSelectedModules().length > 0) || 
			(wizard != null && wizard.canFinish());
	}
	
	@Override
	public boolean performFinish() {
		if (wizard != null) {
			// The finish of the child wizard will get called directly.
			// We only get here if it completed successfully
			if (isNewLocation) {
				KnownRepositories.getInstance().addRepository(location, true /* broadcast */);
			}
			return true;
		} else {
			try {
				new CheckoutMultipleProjectsOperation(getPart(), getSelectedModules(), null)
					.run();
				if (isNewLocation) {
					KnownRepositories.getInstance().addRepository(location, true /* broadcast */);
				}
				return true;
			} catch (InvocationTargetException e) {
				CVSUIPlugin.openError(getShell(), null, null, e);
			} catch (InterruptedException e) {
				// Canceled. fall through.
			}
			return false;
		}
	}
	
	private IWorkbenchPart getPart() {
		// This wizard doesn't have a part
		return null;
	}

	@Override
	public boolean performCancel() {
		if (location != null && isNewLocation) {
			KnownRepositories.getInstance().disposeRepository(location);
			location = null;
		}
		return wizard == null || wizard.performCancel();
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		// Assume the page is about to be shown when this method is
		// invoked
		return getNextPage(page, true /* about to show*/);
	}
	
	@Override
	public IWizardPage getNextPage(IWizardPage page, boolean aboutToShow) {
		if (page == locationPage) {
			if (locationPage.getLocation() == null) {
				return createLocationPage;
			} else {
				if (aboutToShow) {
					try {
						modulePage.setLocation(getLocation());
					} catch (TeamException e1) {
						CVSUIPlugin.log(e1);
					}
				}
				return modulePage;
			}
		}
		if (page == createLocationPage) {
			if (aboutToShow) {
				try {
					ICVSRepositoryLocation l = getLocation();
					if (l != null) {
						modulePage.setLocation(l);
					}
				} catch (TeamException e1) {
					CVSUIPlugin.log(e1);
				}
			}
			return modulePage;
		}
		if (page == modulePage) {
			ICVSRemoteFolder[] selectedModules = getSelectedModules();
			if (selectedModules.length == 0) return null;
			for (ICVSRemoteFolder folder : selectedModules) {
				if (folder.isDefinedModule()) {
					// No further configuration is possible for defined modules
					return null;
				}
			}
			if (aboutToShow) {
				try {
					boolean hasMetafile = true;
					if (selectedModules.length == 1) {
						// Only allow configuration if one module is selected
						final ICVSRemoteFolder[] folders = new ICVSRemoteFolder[] {selectedModules[0]};
						final boolean withName = CVSUIPlugin.getPlugin().isUseProjectNameOnCheckout();

						// attempt to retrieve the project description depending on preferences
						// this is a bit convoluted to batch the meta-file check and retrieval in one operation
						final ICVSRemoteFolder[] folderResult = new ICVSRemoteFolder [1];
						final boolean[] booleanResult = new boolean[] { true };
						
						getContainer().run(true, true, monitor -> {
							ProjectMetaFileOperation op = new ProjectMetaFileOperation(getPart(),
									new ICVSRemoteFolder[] { folders[0] }, withName);
							op.run(monitor);
							folderResult[0] = op.getUpdatedFolders()[0];
							booleanResult[0] = op.metaFileExists();
						});
						hasMetafile = booleanResult[0];
						if (withName && hasMetafile)
							selectedModules[0] = folderResult[0];
					}
					resetSubwizard();
					wizard = new CheckoutAsWizard(getPart(), selectedModules, ! hasMetafile /* allow configuration */);
					wizard.addPages();
					return wizard.getStartingPage();
				} catch (InvocationTargetException e) {
					// Show the error and fall through to return null as the next page
					CVSUIPlugin.openError(getShell(), null, null, e);
				} catch (InterruptedException e) {
					// Canceled by user. Fall through and return null
				}
				return null;
			} else {
				if (wizard == null) {
					return dummyPage;
				} else {
					return wizard.getStartingPage();
				}
			}
		}
		if (wizard != null) {
			return wizard.getNextPage(page);
		}
		return null;
	}

	private ICVSRemoteFolder[] getSelectedModules() {
		if (modulePage == null) return null;
		return modulePage.getSelectedModules();
	}

	/**
	 * Return an ICVSRepositoryLocation
	 */
	private ICVSRepositoryLocation getLocation() throws TeamException {
		// If the location page has a location, use it.
		if (locationPage != null) {
			ICVSRepositoryLocation newLocation = locationPage.getLocation();
			if (newLocation != null) {
				return recordLocation(newLocation);
			}
		}
		
		// Otherwise, get the location from the create location page
		final ICVSRepositoryLocation[] locations = new ICVSRepositoryLocation[] { null };
		final CVSException[] exception = new CVSException[] { null };
		getShell().getDisplay().syncExec(() -> {
			try {
				locations[0] = createLocationPage.getLocation();
			} catch (CVSException e) {
				exception[0] = e;
			}
		});
		if (exception[0] != null) {
			throw exception[0];
		}
		return recordLocation(locations[0]);
	}

	private ICVSRepositoryLocation recordLocation(ICVSRepositoryLocation newLocation) {
		if (newLocation == null) return location;
		if (location == null || !newLocation.equals(location)) {
			if (location != null && isNewLocation) {
				// Dispose of the previous location
				KnownRepositories.getInstance().disposeRepository(location);
			}
			location = newLocation;
			isNewLocation = !KnownRepositories.getInstance().isKnownRepository(newLocation.getLocation(false));
			if (isNewLocation) {
				// Add the location silently so we can work with it
				location = KnownRepositories.getInstance().addRepository(location, false /* silently */);
			}
		}
		return location;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	/*
	 * Reset the sub-wizard
	 */
	/* package */ void resetSubwizard() {
		if (wizard != null) {
			wizard.dispose();
			wizard = null;
		}
	}
}
