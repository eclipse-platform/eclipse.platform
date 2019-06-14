/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolder;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.operations.CheckoutSingleProjectOperation;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class CVSProjectSetSerializer implements IProjectSetSerializer {

	@Override
	public String[] asReference(IProject[] providerProjects, Object context, IProgressMonitor monitor) throws TeamException {
		String[] result = new String[providerProjects.length];
		for (int i = 0; i < providerProjects.length; i++) {
			StringBuilder buffer = new StringBuilder();
			buffer.append("1.0,"); //$NON-NLS-1$
			
			IProject project = providerProjects[i];
			CVSTeamProvider provider = (CVSTeamProvider)RepositoryProvider.getProvider(project);
			CVSWorkspaceRoot root = provider.getCVSWorkspaceRoot();
			CVSRepositoryLocation location = CVSRepositoryLocation.fromString(root.getRemoteLocation().getLocation(false));
			location.setUserMuteable(true);
			String repoLocation = location.getLocation(false);
			buffer.append(repoLocation);
			buffer.append(","); //$NON-NLS-1$
			
			ICVSFolder folder = root.getLocalRoot();
			FolderSyncInfo syncInfo = folder.getFolderSyncInfo();
			String module = syncInfo.getRepository();
			buffer.append(module);
			buffer.append(","); //$NON-NLS-1$
			
			String projectName = folder.getName();
			buffer.append(projectName);
			CVSTag tag = syncInfo.getTag();
			if (tag != null) {
				if (tag.getType() != CVSTag.DATE) {
					buffer.append(","); //$NON-NLS-1$
					String tagName = tag.getName();
					buffer.append(tagName);
				}
			}
			result[i] = buffer.toString();
		}
		return result;
	}

	@Override
	public IProject[] addToWorkspace(String[] referenceStrings, String filename, Object context, IProgressMonitor monitor) throws TeamException {
		final int size = referenceStrings.length;
		final IProject[] projects = new IProject[size];
		final ICVSRepositoryLocation[] locations = new ICVSRepositoryLocation[size];
		final String[] modules = new String[size];
		final CVSTag[] tags = new CVSTag[size];
		
		for (int i = 0; i < size; i++) {
			StringTokenizer tokenizer = new StringTokenizer(referenceStrings[i], ","); //$NON-NLS-1$
			String version = tokenizer.nextToken();
			if (!version.equals("1.0")) { //$NON-NLS-1$
				// Bail out, this is a newer version
				return null;
			}
			String repo = tokenizer.nextToken();
			locations[i] = getLocationFromString(repo);
			modules[i] = tokenizer.nextToken();
			String projectName = tokenizer.nextToken();
			projects[i] = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			if (tokenizer.hasMoreTokens()) {
				String tagName = tokenizer.nextToken();
				tags[i] = new CVSTag(tagName, CVSTag.BRANCH);
			}
		}
		// Check if any projects will be overwritten, and warn the user.
		boolean yesToAll = false;
		int action;
		final int[] num = new int[] {size};
		for (int i = 0; i < size; i++) {
			Shell shell = null;
			IProject project = projects[i];
			if (project.exists()) {
				if (shell == null) {
					if (context instanceof Shell) {
						shell = (Shell)context;
					} else {
						return null;
					}
				}
				action = confirmOverwrite(project, yesToAll, shell);
				yesToAll = action == 2;
				
				// message dialog
					switch (action) {
						// no
						case 1:
							// Remove it from the set
							locations[i] = null;
							num[0]--;
							break;
						// yes to all
						case 2:
						// yes
						case 0:
							break;
						// cancel
						case 3:
						default:
							return null;
					}
			}
		}
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			@Override
			public void execute(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
				monitor.beginTask("", 1000 * num[0]); //$NON-NLS-1$
				try {
					for (int i = 0; i < size; i++) {
						if (locations[i] != null) {
							ICVSRemoteFolder remote = new RemoteFolder(null, locations[i], modules[i], tags[i]);
							new CheckoutSingleProjectOperation(null /* no part */, remote, projects[i], null /* location */, true)
								.run(SubMonitor.convert(monitor, 1000));
						}
					}
				} finally {
					monitor.done();
				}
			}
		};
		try {
			op.run(monitor);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t instanceof TeamException) {
				throw (TeamException)t;
			}
		}
		List result = new ArrayList();
		for (IProject project : projects) {
			if (project != null) {
				result.add(project);
			}
		}
		return (IProject[])result.toArray(new IProject[result.size()]);
	}

	private ICVSRepositoryLocation getLocationFromString(String repo) throws CVSException {
		// create the new location
		ICVSRepositoryLocation newLocation = CVSRepositoryLocation.fromString(repo);
		if (newLocation.getUsername() == null || newLocation.getUsername().length() == 0) {
			// look for an existing location that matched
			ICVSRepositoryLocation[] locations = KnownRepositories.getInstance().getRepositories();
			for (ICVSRepositoryLocation location : locations) {
				if (location.getMethod() == newLocation.getMethod()
						&& location.getHost().equals(newLocation.getHost())
						&& location.getPort() == newLocation.getPort()
						&& location.getRootDirectory().equals(newLocation.getRootDirectory()))
					return location;
			}
		}
		return newLocation;
	}
	
	private int confirmOverwrite(IProject project, boolean yesToAll, Shell shell) {
		if (yesToAll) return 2;
		if (!project.exists()) return 0;
		final MessageDialog dialog = 
			new MessageDialog(shell, CVSUIMessages.CVSProjectSetSerializer_Confirm_Overwrite_Project_8, null, NLS.bind(CVSUIMessages.CVSProjectSetSerializer_The_project__0__already_exists__Do_you_wish_to_overwrite_it__9, new String[] { project.getName() }), MessageDialog.QUESTION, // 
				new String[] {
					IDialogConstants.YES_LABEL,
					IDialogConstants.NO_LABEL,
					IDialogConstants.YES_TO_ALL_LABEL, 
					IDialogConstants.CANCEL_LABEL}, 
				0);
		final int[] result = new int[1];
		shell.getDisplay().syncExec(() -> result[0] = dialog.open());
		return result[0];
	}
}
