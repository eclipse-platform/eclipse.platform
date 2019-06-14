/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

import org.eclipse.core.resources.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.operations.*;
import org.eclipse.team.internal.ccvs.ui.tags.*;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class CheckoutAsWizard extends Wizard {
	
	private ICVSRemoteFolder[] remoteFolders;
	private boolean allowProjectConfiguration;

	private CheckoutAsMainPage mainPage;
	private CheckoutAsProjectSelectionPage projectSelectionPage;
	private CheckoutAsLocationSelectionPage locationSelectionPage;
	private TagSelectionWizardPage tagSelectionPage;
	private IWorkbenchPart part;

	class NewProjectListener implements IResourceChangeListener {
		private IProject newProject = null;

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta root = event.getDelta();
			IResourceDelta[] projectDeltas = root.getAffectedChildren();
			for (IResourceDelta delta : projectDeltas) {
				IResource resource = delta.getResource();
				if (delta.getKind() == IResourceDelta.ADDED) {
					newProject = (IProject)resource;
				}
			}
		}
		/**
		 * Gets the newProject.
		 * @return Returns a IProject
		 */
		public IProject getNewProject() {
			return newProject;
		}
	}
	
	/**
	 * Return the settings used for all CheckoutAsWizard pages
	 */
	public static IDialogSettings getCheckoutAsDialogSettings() {
		IDialogSettings workbenchSettings = CVSUIPlugin.getPlugin().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("CheckoutAsWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("CheckoutAsWizard");//$NON-NLS-1$
		}
		return section;
	}
	
	public CheckoutAsWizard(IWorkbenchPart part, ICVSRemoteFolder[] remoteFolders, boolean allowProjectConfiguration) {
		this.part = part;
		this.remoteFolders = remoteFolders;
		setDialogSettings(getCheckoutAsDialogSettings());
		setWindowTitle(CVSUIMessages.CheckoutAsWizard_title); 
		this.allowProjectConfiguration = allowProjectConfiguration;
	}
	
	@Override
	public void addPages() {
		setNeedsProgressMonitor(true);
		ImageDescriptor substImage = CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_CHECKOUT);
		
		mainPage = new CheckoutAsMainPage(substImage, remoteFolders, allowProjectConfiguration);
		addPage(mainPage);
		
		projectSelectionPage = new CheckoutAsProjectSelectionPage(substImage, remoteFolders);
		addPage(projectSelectionPage);
		
		locationSelectionPage = new CheckoutAsLocationSelectionPage(substImage, remoteFolders);
		addPage(locationSelectionPage);
		
		tagSelectionPage = new TagSelectionWizardPage("tagPage", CVSUIMessages.CheckoutAsWizard_3, substImage, CVSUIMessages.CheckoutAsWizard_4, TagSource.create(remoteFolders), //$NON-NLS-1$    
				TagSelectionArea.INCLUDE_HEAD_TAG |
				TagSelectionArea.INCLUDE_BRANCHES |
				TagSelectionArea.INCLUDE_VERSIONS |
				TagSelectionArea.INCLUDE_DATES
				);
		if (remoteFolders.length > 0) {
			try {
				CVSTag selectedTag = remoteFolders[0].getFolderSyncInfo().getTag();
				tagSelectionPage.setSelection(selectedTag);
			} catch (CVSException e) {
				CVSUIPlugin.log(e);
			}
		}
		tagSelectionPage.setHelpContxtId(IHelpContextIds.CHECKOUT_TAG_SELETION_PAGE);
		addPage(tagSelectionPage);
	}

	@Override
	public boolean performFinish() {
		try {
			if (mainPage.isPerformConfigure()) {
				return performConfigureAndCheckout();
			} else if (mainPage.isPerformCheckoutAs()) {
				if (isSingleFolder()) {
					return performSingleCheckoutAs();
				} else {
					return performMultipleCheckoutAs();
				}
			} else if (mainPage.isPerformCheckoutInto()) {
				return performCheckoutInto();
			}
		} catch (InvocationTargetException e) {
			handle(e);
			// drop through
		} catch (InterruptedException e) {
			// drop through
		}
		return false;
	}

	private boolean isSingleFolder() {
		return remoteFolders.length == 1;
	}
	
	@Override
	public boolean canFinish() {
		return (mainPage.isPageComplete() 
		&& (mainPage.isPerformConfigure()
			|| (mainPage.isPerformCheckoutInto() && projectSelectionPage.isPageComplete()) 
			|| (mainPage.isPerformCheckoutAs() && locationSelectionPage.isPageComplete())));
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == mainPage) {
			if (mainPage.isPerformConfigure()) return tagSelectionPage;
			if (mainPage.isPerformCheckoutInto()) return projectSelectionPage;
			if (mainPage.isPerformCheckoutAs()) {
				if (isSingleFolder()) {
					locationSelectionPage.setProjectName(mainPage.getProjectName());
				} else {
					locationSelectionPage.setProject(null);
				}
				return locationSelectionPage; 
			} 
		}
		// The tag selection page is always shown as the last page
		if (page != tagSelectionPage) {
			return tagSelectionPage;
		}
		return null;
	}
	
	private void handle(Throwable e) {
		CVSUIPlugin.openError(getShell(), CVSUIMessages.CheckoutAsWizard_error, null, e); 
	}
	
	/*
	 * Configure a local project and checkout the selected remote folder into the project.
	 * This only occurs for single folders.
	 */
	private boolean performConfigureAndCheckout() throws InvocationTargetException, InterruptedException {
		IProject newProject = getNewProject();
		if (newProject == null) return false;
		// Run the checkout in the background
		ICVSRemoteFolder folder = getRemoteFolder();
		new CheckoutSingleProjectOperation(part, folder, newProject, null, true).run();
		return true;
	}
	
	/*
	 * Return the single remote folder to be checked out
	 */
	private ICVSRemoteFolder getRemoteFolder() {
		ICVSRemoteFolder folder = remoteFolders[0];
		CVSTag selectedTag = getSelectedTag();
		if (selectedTag != null) {
			folder = (ICVSRemoteFolder)folder.forTag(selectedTag);
		}
		return folder;
	}
	
	/*
	 * Return the remote folders to be checked out
	 */
	private ICVSRemoteFolder[] getRemoteFolders() {
		ICVSRemoteFolder[] folders = remoteFolders.clone();
		CVSTag selectedTag = getSelectedTag();
		// see bug 160851
		if(selectedTag != null){
			for (int i = 0; i < remoteFolders.length; i++) {
				folders[i] = (ICVSRemoteFolder)folders[i].forTag(getSelectedTag());
			}
		}
		return folders;
	}

	/*
	 * Return the remote folders to be checked out with 
	 * Folder description if available based on preferrences settings
	 */
	private ICVSRemoteFolder[] getRemoteFoldersWithProjectDescriptions() throws InvocationTargetException, InterruptedException {
		ICVSRemoteFolder[] folders = getRemoteFolders();
		if (CVSUIPlugin.getPlugin().isUseProjectNameOnCheckout()) {
			folders = ProjectMetaFileOperation.updateFoldersWithProjectName(part, folders);
		}
		return folders;
	}

	private CVSTag getSelectedTag() {
		return tagSelectionPage.getSelectedTag();
	}

	private boolean performSingleCheckoutAs() throws InvocationTargetException, InterruptedException {
		IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(mainPage.getProjectName());
		String targetLocation = locationSelectionPage.getTargetLocation();
		// Run the checkout in the background
		ICVSRemoteFolder folder = getRemoteFolder();
		final boolean recurse = mainPage.isRecurse();
		new CheckoutSingleProjectOperation(part, folder, newProject, targetLocation, false, mainPage.getWorkingSets()) {
				@Override
				protected boolean isRecursive() {
					return recurse;
				}
			}.run();
		return true;
	}

	/**
	 * Check out multiple folders to the workspace using a custom location if one is
	 * specified.
	 */
	private boolean performMultipleCheckoutAs() throws InvocationTargetException, InterruptedException {
		String targetLocation = locationSelectionPage.getTargetLocation();
		// Run the checkout in the background
		new CheckoutMultipleProjectsOperation(part, getRemoteFoldersWithProjectDescriptions(), targetLocation, mainPage.getWorkingSets()).run();
		return true;
	}

	private boolean performCheckoutInto() throws InvocationTargetException, InterruptedException {
		CheckoutIntoOperation operation;
		boolean recursive = mainPage.isRecurse();
		if (isSingleFolder()) {
			ICVSRemoteFolder folder = getRemoteFolder();
			operation = new CheckoutIntoOperation(part, folder, projectSelectionPage.getLocalFolder(), recursive);
		} else {
			operation = new CheckoutIntoOperation(part, getRemoteFolders(), projectSelectionPage.getParentFolder(), recursive);
		}
		// Run the checkout in the background
		operation.run();
		return true;
	}

	/**
	 * Get a new project that is configured by the new project wizard.
	 * This is currently the only way to do this.
	 */
	private IProject getNewProject() {
		NewProjectListener listener = new NewProjectListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
		(new NewProjectAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow())).run();
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
		IProject project = listener.getNewProject();
		return project;
	}

}
