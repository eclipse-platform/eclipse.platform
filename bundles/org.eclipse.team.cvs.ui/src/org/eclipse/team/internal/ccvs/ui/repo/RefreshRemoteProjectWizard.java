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
package org.eclipse.team.internal.ccvs.ui.repo;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;

/**
 * Wizard for refreshing the tags for a CVS repository location
 */
public class RefreshRemoteProjectWizard extends Wizard {
	
	// The initial size of this wizard.
	private final static int INITIAL_WIDTH = 300;
	private final static int INITIAL_HEIGHT = 350;
	
	private ICVSRepositoryLocation root;
	private ICVSRemoteResource[] rootFolders;
	private RefreshRemoteProjectSelectionPage projectSelectionPage;
	private IDialogSettings settings;
	
	public static boolean execute(Shell shell, final ICVSRepositoryLocation root) {
		final ICVSRemoteResource[][] rootFolders = new ICVSRemoteResource[1][0];
		rootFolders[0] = null;
		try {
			new ProgressMonitorDialog(shell).run(true, true, monitor -> {
				try {
					rootFolders[0] = CVSUIPlugin.getPlugin().getRepositoryManager().getFoldersForTag(root,
							CVSTag.DEFAULT, monitor);
				} catch (CVSException e) {
					throw new InvocationTargetException(e);
				}
			});
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(shell, null, null, e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		RefreshRemoteProjectWizard wizard = new RefreshRemoteProjectWizard(root, rootFolders[0]);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		/**
		 * This is the only place where a size hint > 0 is required. The wizard
		 * page should in general have hints of 0 (and grab excessive space).
		 */
		dialog.setMinimumPageSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		return (dialog.open() == Window.OK);
	}
	
	public RefreshRemoteProjectWizard(ICVSRepositoryLocation root, ICVSRemoteResource[] rootFolders) {
		this.root = root;
		this.rootFolders = rootFolders;
		IDialogSettings workbenchSettings = CVSUIPlugin.getPlugin().getDialogSettings();
		this.settings = workbenchSettings.getSection("RefreshRemoteProjectWizard");//$NON-NLS-1$
		if (settings == null) {
			this.settings = workbenchSettings.addNewSection("RefreshRemoteProjectWizard");//$NON-NLS-1$
		}
		setWindowTitle(CVSUIMessages.RefreshRemoteProjectWizard_title); 
	}
	
	@Override
	public void addPages() {
		setNeedsProgressMonitor(true);
		ImageDescriptor substImage = CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_NEW_LOCATION);
		projectSelectionPage = new RefreshRemoteProjectSelectionPage(
			"ProjectSelectionPage", //$NON-NLS-1$
			CVSUIMessages.RefreshRemoteProjectSelectionPage_pageTitle, 
			substImage,
			CVSUIMessages.RefreshRemoteProjectSelectionPage_pageDescription, 
			settings, root, rootFolders);
		addPage(projectSelectionPage);
	}
	
	@Override
	public boolean performFinish() {
		final ICVSRemoteResource[] selectedFolders = projectSelectionPage.getSelectedRemoteProject();
		try {
			getContainer().run(true, true, monitor -> {
				final RepositoryManager manager = CVSUIPlugin.getPlugin().getRepositoryManager();
				// Run in the manager to avoid multiple repo view updates
				manager.run(monitor1 -> {
					monitor1.beginTask(null, 100);
					ICVSRemoteResource[] failedFolders = internalRefresh(manager, selectedFolders, false /* recurse */,
							Policy.subMonitorFor(monitor1, 80));
					if (failedFolders.length > 0) {
						// Go deep any any failed folders.
						if (promptForDeepRefresh(failedFolders))
							internalRefresh(manager, failedFolders, true /* recurse */,
									Policy.subMonitorFor(monitor1, 20));
					}
					monitor1.done();
				}, monitor);
			});
			return true;
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(getShell(), null, null ,e);
		} catch (InterruptedException e) {
		}
		return false;
	}

	/*
	 * Refresh the tags of the given resources and return those for which no tags were found.
	 */
	private ICVSRemoteResource[] internalRefresh(final RepositoryManager manager, final ICVSRemoteResource[] selectedFolders, final boolean recurse, IProgressMonitor monitor) throws InvocationTargetException {
		List failedFolders = new ArrayList();
		monitor.beginTask(null, 100 * selectedFolders.length);
		for (ICVSRemoteResource selectedFolder : selectedFolders) {
			try {
				ICVSRemoteResource resource = selectedFolder;
				if (resource instanceof ICVSFolder) {
					CVSTag[] tags = manager.refreshDefinedTags((ICVSFolder)resource, recurse, true /* notify */, Policy.subMonitorFor(monitor, 100));
					if (tags.length == 0) {
						failedFolders.add(resource);
					}
				}
			} catch (TeamException e) {
				CVSUIPlugin.log(IStatus.ERROR, NLS.bind("An error occurred while fetching the tags for {0}", selectedFolder.getName()), e); //$NON-NLS-1$
			}
		}
			return (ICVSRemoteResource[]) failedFolders.toArray(new ICVSRemoteResource[failedFolders.size()]);
	}

	private boolean promptForDeepRefresh(final ICVSRemoteResource[] folders) {
		final boolean[] prompt = new boolean[] { false };
		getShell().getDisplay().syncExec(() -> {
			MessageDialog dialog = new MessageDialog(getShell(), CVSUIMessages.RefreshRemoteProjectWizard_0, null,
					getNoTagsMessage(folders), MessageDialog.INFORMATION, new String[] {
							CVSUIMessages.RefreshRemoteProjectWizard_1, CVSUIMessages.RefreshRemoteProjectWizard_2 },
					1);
			int code = dialog.open();
			if (code == 0) {
				prompt[0] = true;
			}

		});
		return prompt[0];
	}

	private String getNoTagsMessage(ICVSRemoteResource[] folders) {
		if (folders.length == 1) {
			return NLS.bind(CVSUIMessages.RefreshRemoteProjectWizard_3, new String[] { folders[0].getRepositoryRelativePath() }); 
		}
		return NLS.bind(CVSUIMessages.RefreshRemoteProjectWizard_4, new String[] { Integer.toString(folders.length) }); 
	}
}
