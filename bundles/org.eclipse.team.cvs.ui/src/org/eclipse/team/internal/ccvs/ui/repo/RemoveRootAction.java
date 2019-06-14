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
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.model.RepositoryLocationSchedulingRule;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.internal.ui.dialogs.DetailsDialogWithProjects;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.progress.IProgressService;


/**
 * RemoveRootAction removes a repository
 */
public class RemoveRootAction extends SelectionListenerAction {
	private IStructuredSelection selection;
	/* internal use only */ Shell shell;
	private RepositoriesView view;
	
	public RemoveRootAction(Shell shell, RepositoriesView view) {
		super(CVSUIMessages.RemoteRootAction_label); 
		this.view = view;
		this.shell = shell;
	}
	
	/**
	 * Returns the selected remote files
	 */
	protected ICVSRepositoryLocation[] getSelectedRemoteRoots() {
		ArrayList resources = null;
		if (selection!=null && !selection.isEmpty()) {
			resources = new ArrayList();
			Iterator elements = selection.iterator();
			while (elements.hasNext()) {
				Object next = TeamAction.getAdapter(elements.next(), RepositoryRoot.class);
				if (next instanceof RepositoryRoot) {
					resources.add(((RepositoryRoot)next).getRoot());
				}
			}
		}
		if (resources != null && !resources.isEmpty()) {
			ICVSRepositoryLocation[] result = new ICVSRepositoryLocation[resources.size()];
			resources.toArray(result);
			return result;
		}
		return new ICVSRepositoryLocation[0];
	}

	@Override
	public void run() {
		final ICVSRepositoryLocation[] roots = getSelectedRemoteRoots();
		if (roots.length == 0) return;
		final boolean[] proceed = new boolean[1];
		shell.getDisplay().syncExec(() -> {
			String message;
			if (roots.length == 1) {
				message = NLS.bind(CVSUIMessages.RemoveRootAction_RepositoryRemovalDialogMessageSingle,
						roots[0].getLocation(true));
			} else {
				message = NLS.bind(CVSUIMessages.RemoveRootAction_RepositoryRemovalDialogMessageMultiple,
						Integer.valueOf(roots.length));
			}
			proceed[0] = MessageDialog.openQuestion(shell, CVSUIMessages.RemoveRootAction_RepositoryRemovalDialogTitle,
					message);
		});
		if(!proceed[0]){
			return;
		}
		for (ICVSRepositoryLocation root : roots) {
			try {	
				// Check if any projects are shared with the repository
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				final ArrayList shared = new ArrayList();
				for (IProject project : projects) {
					RepositoryProvider teamProvider = RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId());
					if (teamProvider!=null) {
						CVSTeamProvider cvsProvider = (CVSTeamProvider)teamProvider;
						if (cvsProvider.getCVSWorkspaceRoot().getRemoteLocation().equals(root)) {
							shared.add(project);
						}
					}
				}
			
				// This will notify the RepositoryManager of the removal
				if (!shared.isEmpty()) {
					final String location = root.getLocation(true);
					shell.getDisplay().syncExec(() -> {
						DetailsDialogWithProjects dialog = new DetailsDialogWithProjects(shell,
								CVSUIMessages.RemoteRootAction_Unable_to_Discard_Location_1,
								NLS.bind(
										CVSUIMessages.RemoteRootAction_Projects_in_the_local_workspace_are_shared_with__2,
										new String[] { location }),
								CVSUIMessages.RemoteRootAction_The_projects_that_are_shared_with_the_above_repository_are__4,
								(IProject[]) shared.toArray(new IProject[shared.size()]), false, Dialog.DLG_IMG_ERROR);
						dialog.open();
					});
				} else {
					IProgressService manager = PlatformUI.getWorkbench().getProgressService();
					try {
						manager.busyCursorWhile(monitor -> {
							final ISchedulingRule rule = new RepositoryLocationSchedulingRule(root);
							try {
								Job.getJobManager().beginRule(rule, monitor);
								view.getContentProvider().cancelJobs(root);
								KnownRepositories.getInstance().disposeRepository(root);
							} finally {
								Job.getJobManager().endRule(rule);
							}

						});
					} catch (InvocationTargetException e) {
						throw CVSException.wrapException(e);
					} catch (InterruptedException e) {
						// Canceled
						return;
					}
				}
			} catch (CVSException e) {
				CVSUIPlugin.openError(view.getShell(), null, null, e, CVSUIPlugin.PERFORM_SYNC_EXEC | CVSUIPlugin.LOG_TEAM_EXCEPTIONS | CVSUIPlugin.LOG_NONTEAM_EXCEPTIONS);
			}
		}
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		this.selection = selection;
		ICVSRepositoryLocation[] roots = getSelectedRemoteRoots();
		boolean b = roots.length > 0;
		setEnabled(b);
		return b;
	}

}

