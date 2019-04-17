/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.*;
import org.eclipse.team.internal.ccvs.ui.tags.*;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IWorkbench;

/**
 * This wizard helps the user to import a new project in their workspace
 * into a CVS repository for the first time.
 */
public class SharingWizard extends Wizard implements IConfigurationWizard, ICVSWizard {
	// The project to configure
	private IProject project;

	// The autoconnect page is used if CVS/ directories already exist.
	private ConfigurationWizardAutoconnectPage autoconnectPage;
	
	// The import page is used if CVS/ directories do not exist.
	private RepositorySelectionPage locationPage;
	
	// The page that prompts the user for connection information.
	private ConfigurationWizardMainPage createLocationPage;
	
	// The page that prompts the user for module name.
	private ModuleSelectionPage modulePage;

	// The page that lets the user pick a branch to share against
	private TagSelectionWizardPage tagPage;
	
	// The page that allows the user to commit or update resources
	private SharingWizardSyncPage syncPage;
	
	// Keep track of location state so we know what to do at the end
	private ICVSRepositoryLocation location;
	private boolean isNewLocation;
	
	// Keep track of the folder that existed the last time we checked
	private ICVSRemoteFolder existingRemote;

	private ICVSRemoteFolder remoteFolder;
	
	public SharingWizard() {
		IDialogSettings cvsSettings = CVSUIPlugin.getPlugin().getDialogSettings();
		IDialogSettings section = cvsSettings.getSection("SharingWizard");//$NON-NLS-1$
		if (section == null) {
			section = cvsSettings.addNewSection("SharingWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
		setNeedsProgressMonitor(true);
		setWindowTitle(CVSUIMessages.SharingWizard_title); 
	}	
		
	@Override
	public void addPages() {
		ImageDescriptor sharingImage = CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_SHARE);
		boolean autoconnect = false;
		if (doesCVSDirectoryExist()) {
			autoconnectPage = new ConfigurationWizardAutoconnectPage("autoconnectPage", CVSUIMessages.SharingWizard_autoConnectTitle, sharingImage); //$NON-NLS-1$ 
			if (autoconnectPage.setProject(project)) {
				autoconnectPage.setDescription(CVSUIMessages.SharingWizard_autoConnectTitleDescription); 
				addPage(autoconnectPage);
				autoconnect = true;
			}
		}
		if (!autoconnect) {
			ICVSRepositoryLocation[] locations = CVSUIPlugin.getPlugin().getRepositoryManager().getKnownRepositoryLocations();
			if (locations.length > 0) {
				locationPage = new RepositorySelectionPage("importPage", CVSUIMessages.SharingWizard_importTitle, sharingImage); //$NON-NLS-1$ 
				locationPage.setDescription(CVSUIMessages.SharingWizard_importTitleDescription); 
				addPage(locationPage);
			}
			createLocationPage = new ConfigurationWizardMainPage("createLocationPage", CVSUIMessages.SharingWizard_enterInformation, sharingImage); //$NON-NLS-1$ 
			createLocationPage.setDescription(CVSUIMessages.SharingWizard_enterInformationDescription); 
			createLocationPage.setCVSWizard(this);
			createLocationPage.setDialogSettings(NewLocationWizard.getLocationDialogSettings());
			addPage(createLocationPage);
			modulePage = new ModuleSelectionPage("modulePage", CVSUIMessages.SharingWizard_enterModuleName, sharingImage); //$NON-NLS-1$ 
			modulePage.setDescription(CVSUIMessages.SharingWizard_enterModuleNameDescription); 
			modulePage.setCVSWizard(this);
			modulePage.setProject(project);
			modulePage.setHelpContxtId(IHelpContextIds.SHARING_MODULE_PAGE);
			addPage(modulePage);
			
			addTagPage(sharingImage);
			addSyncPage(sharingImage);
		}
	}
	
	private void addTagPage(ImageDescriptor sharingImage) {
		tagPage = new TagSelectionWizardPage("tagPage",  //$NON-NLS-1$
			CVSUIMessages.SharingWizard_selectTagTitle,  
			sharingImage,
			CVSUIMessages.SharingWizard_selectTag, 
			TagSource.EMPTY, // start with an empty tag source
			TagSourceWorkbenchAdapter.INCLUDE_HEAD_TAG | TagSourceWorkbenchAdapter.INCLUDE_BRANCHES); 
		tagPage.setCVSWizard(this);
		tagPage.setHelpContxtId(IHelpContextIds.SHARING_TAG_SELETION_PAGE);
		addPage(tagPage);
	}
	
	private void addSyncPage(ImageDescriptor sharingImage) {
		syncPage = new SharingWizardSyncPage("syncPagePage",  //$NON-NLS-1$
			CVSUIMessages.SharingWizard_23,  
			sharingImage,
			CVSUIMessages.SharingWizard_24); 
		syncPage.setProject(project);
		syncPage.setCVSWizard(this);
		addPage(syncPage);
	}
	
	@Override
	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		return (page == autoconnectPage || page == syncPage);
	}
	
	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		// Assume the page is about to be shown when this method is
		// invoked
		return getNextPage(page, true /* about to show*/);
	}
	
	@Override
	public IWizardPage getNextPage(IWizardPage page, boolean aboutToShow) {
		if (page == autoconnectPage) return null;
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
					modulePage.setLocation(getLocation());
				} catch (TeamException e1) {
					CVSUIPlugin.log(e1);
				}
			}
			return modulePage;
		}
		try {
			if (page == modulePage) {
				if (aboutToShow) {
					remoteFolder = null; // reset
					getRemoteFolder(); // initialize remoteFolder
					if (exists(remoteFolder)) {
						prepareTagPage(remoteFolder);
						return tagPage;
					} else {
						try {
							populateSyncPage(false /* remote doesn't exist */);
						} catch (InvocationTargetException e) {
							CVSUIPlugin.openError(getShell(), null, null, e);
							if (!RepositoryProvider.isShared(project)) {
								// Only stay on the current page if the sharing was a total failure
								return null;
							}
						}
						return syncPage;
					}
				} else {
					return syncPage;
				}
			}
			if (page == tagPage) {
				if (aboutToShow) {
					remoteFolder = null; // reset
					getRemoteFolder(); // initialize remoteFolder
					populateSyncPage(true /* remote exists */);
				}
				return syncPage;
			}
		} catch (InvocationTargetException e) {
			// Show the error and fall through to return null as the next page
			CVSUIPlugin.openError(getShell(), null, null, e);
		} catch (InterruptedException e) {
			// The user cancelled. Fall through and return null as the next page.
		}
		return null;
	}

	@Override
	public boolean performFinish() {
		final boolean[] result = new boolean[] { true };
		if (isAutoconnect()) {
			try {
				getContainer().run(true /* fork */, true /* cancel */, monitor -> {
					try {
						result[0] = autoconnectCVSProject(monitor);
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
				});
			} catch (InterruptedException e) {
				return true;
			} catch (InvocationTargetException e) {
				CVSUIPlugin.openError(getContainer().getShell(), null, null, e);
				result[0] = false;
			}
		}
		// Add the location to the provider if it is new
		if (result[0] && isNewLocation) {
			KnownRepositories.getInstance().addRepository(location, true /* broadcast */);
		}
		
		final Shell parentShell= getShell().getParent().getShell();
		if (getContainer().getCurrentPage() == syncPage) {
			syncPage.saveSettings();
			if (syncPage.commitChanges()) {
				Display.getCurrent().asyncExec(() -> {
					try {
						CommitWizard.run(null, parentShell, new IResource[] { syncPage.getProject() });
					} catch (CVSException e) {
						// TODO:handle
						CVSUIPlugin.log(e);
					}
				});
			}
		}
		return result[0];
	}

	@Override
	public boolean performCancel() {
		boolean disposeLocation = isNewLocation;
		ICVSRepositoryLocation location;
		try {
			location = getLocation();
		} catch (TeamException e) {
			CVSUIPlugin.log(e);
			return true;
		}
		if (location == null) return true;
		// If on the last page, offer to disconnect
		if (getContainer().getCurrentPage() == syncPage
				&& RepositoryProvider.getProvider(project) != null) {
			// Prompt to see if we should undo out work
			if (promptToKeepMapping()) {
				// If we didn't disconnect, don't dispose the repo
				disposeLocation = false;
				// Add the location to the provider if it is new
				if (isNewLocation) {
					KnownRepositories.getInstance().addRepository(location, true /* broadcast */);
				}
			} else {
				try {
					getContainer().run(true, true,
							monitor -> new DisconnectOperation(null, new IProject[] { project }, true).run(monitor));
				} catch (InvocationTargetException e) {
					CVSUIPlugin.log(IStatus.ERROR, e.getMessage(), e.getTargetException());
				} catch (InterruptedException e) {
					// Ignore
				}
			}
		}
		// Dispose of the location if appropriate
		if (disposeLocation) {
			KnownRepositories.getInstance().disposeRepository(location);
		}
		return super.performCancel();
	}

	private boolean promptToKeepMapping() {
		return (MessageDialog.openQuestion(getShell(), CVSUIMessages.SharingWizard_26, NLS.bind(CVSUIMessages.SharingWizard_27, new String[] { project.getName() }))); // 
	}

	private void reconcileProject(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		new ReconcileProjectOperation(getShell(), project, getRemoteFolder()).run(monitor);
	}
	
	/**
	 * Return an ICVSRepositoryLocation
	 */
	private ICVSRepositoryLocation getLocation() throws TeamException {
		// If there is an autoconnect page then it has the location
		if (autoconnectPage != null) {
			return recordLocation(autoconnectPage.getLocation());
		}
		
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
	public void init(IWorkbench workbench, IProject project) {
		this.project = project;
	}
	
	private boolean doesCVSDirectoryExist() {
		// Determine if there is an existing CVS/ directory from which configuration
		// information can be retrieved.
		Shell shell = null;
		if (getContainer() != null) {
			shell = getContainer().getShell();
		}
		final boolean[] isCVSFolder = new boolean[] { false };
		try {
			CVSUIPlugin.runWithRefresh(shell, new IResource[] { project }, monitor -> {
				try {
					ICVSFolder folder = (ICVSFolder) CVSWorkspaceRoot.getCVSResourceFor(project);
					FolderSyncInfo info = folder.getFolderSyncInfo();
					isCVSFolder[0] = info != null;
				} catch (final TeamException e) {
					throw new InvocationTargetException(e);
				}
			}, null);
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(shell, null, null, e);
		} catch (InterruptedException e) {
			// Cancelled. Just fall through
		}
		return isCVSFolder[0];
	}
	
	/*
	 * Should the project be auto-connected
	 */
	/* private*/ boolean isAutoconnect() {
		return autoconnectPage != null && doesCVSDirectoryExist();
	}
	
	/*
	 * Auto-connect to the repository using CVS/ directories
	 */
	/*private */ boolean autoconnectCVSProject(IProgressMonitor monitor) throws TeamException {
		try {
			monitor.beginTask(null, 100);
			
			FolderSyncInfo info = autoconnectPage.getFolderSyncInfo();
			if (info == null) {
				// Error!
				return false;
			}
			
			// Get the repository location (the get will add the location to the provider)
			ICVSRepositoryLocation location = getLocation();
	
			// Validate the connection if the user wants to
			boolean validate = autoconnectPage.getValidate();					
			if (validate) {
				// Do the validation
				try {
					location.validateConnection(Policy.subMonitorFor(monitor, 50));
				} catch (final TeamException e) {
					// Exception validating. We can continue if the user wishes.
					final boolean[] keep = new boolean[] { false };
					getShell().getDisplay()
							.syncExec(() -> keep[0] = MessageDialog.openQuestion(getContainer().getShell(),
									CVSUIMessages.SharingWizard_validationFailedTitle,
									NLS.bind(CVSUIMessages.SharingWizard_validationFailedText,
											(new Object[] { e.getStatus().getMessage() }))));
					if (!keep[0]) {
						return false;
					}
					// They want to keep the connection anyway. Fall through.
				}
			}
			
			// Set the sharing
			CVSWorkspaceRoot.setSharing(project, info, Policy.subMonitorFor(monitor, 50));
			return true;
		} finally {
			monitor.done();
		}
	}
	
	private boolean shareProject(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		monitor.beginTask(null, 100);
		ICVSRepositoryLocation location = null;
		try {
			location = getLocation();
			location.validateConnection(Policy.subMonitorFor(monitor, 50));
		} catch (TeamException e) {
			CVSUIPlugin.openError(getShell(), null, null, e, CVSUIPlugin.PERFORM_SYNC_EXEC);
			if (isNewLocation && location != null) location.flushUserInfo();
			return false;
		}
		
		// Create the remote module for the project
		ShareProjectOperation op = new ShareProjectOperation(null, location, project, getRemoteFolder().getRepositoryRelativePath());
		op.setShell(getShell());
		op.run(Policy.subMonitorFor(monitor, 50));
		return true;
	}
	
	private CVSTag getTag() {
		if (tagPage == null || tagPage.getSelectedTag() == null) {
			return CVSTag.DEFAULT;
		}
		return tagPage.getSelectedTag();
	}
	
	private ICVSRemoteFolder getRemoteFolder() {
		if (remoteFolder == null) {
			ICVSRemoteFolder folder = modulePage.getSelectedModule();
			remoteFolder = (ICVSRemoteFolder)folder.forTag(getTag());
		}
		return remoteFolder;
	}
	
	private boolean exists(ICVSRemoteFolder folder, IProgressMonitor monitor) throws TeamException {
		if (existingRemote != null && existingRemote.equals(folder)) return true;
		if (folder.exists(monitor)) {
			existingRemote = folder;
			return true;
		} else {
			existingRemote = null;
			return false;
		}
	}
	
	private boolean exists(final ICVSRemoteFolder folder) throws InvocationTargetException, InterruptedException {
		final boolean[] result = new boolean[] { false };
		getContainer().run(true, true, monitor -> {
			try {
				result[0] = exists(folder, monitor);
			} catch (TeamException e) {
				throw new InvocationTargetException(e);
			}
		});
		return result[0];
	}
	
	private void populateSyncPage(final boolean exists) throws InvocationTargetException, InterruptedException {
		getContainer().run(true, true, monitor -> {
			monitor.beginTask(null, IProgressMonitor.UNKNOWN);
			if (exists) {
				reconcileProject(Policy.subMonitorFor(monitor, 50));
			} else {
				shareProject(Policy.subMonitorFor(monitor, 50));
			}
			try {
				getParticipant().getContext().refresh(Utils.getResourceMappings(new IProject[] { project }),
						Policy.subMonitorFor(monitor, 50));
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
			if (monitor.isCanceled()) {
				throw new InterruptedException();
			}
			monitor.done();
		});
	}
	
	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {
		if (page == syncPage) {
			// There's no going back from the sync page
			return null;
		}
		return super.getPreviousPage(page);
	}
	
	private void prepareTagPage(ICVSRemoteFolder remote) {
		tagPage.setTagSource(TagSource.create(remote));
		tagPage.setDescription(NLS.bind(CVSUIMessages.SharingWizard_25, new String[] { remote.getRepositoryRelativePath() })); 
	}

	private ModelSynchronizeParticipant getParticipant() {
		return syncPage.getParticipant();
	}
}
