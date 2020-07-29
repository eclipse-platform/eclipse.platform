/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.mapping;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.PlatformUI;

/**
 * Remove the selected elements from the page
 */
public class RemoveFromViewAction extends ResourceModelParticipantAction {

	public RemoveFromViewAction(ISynchronizePageConfiguration configuration) {
		super(null, configuration);
		Utils.initAction(this, "action.removeFromView."); //$NON-NLS-1$
		setId(TeamUIPlugin.REMOVE_FROM_VIEW_ACTION_ID);
	}

	@Override
	public void run() {
		if (confirmRemove()) {
			try {
				PlatformUI.getWorkbench().getProgressService().run(true, true, monitor -> {
					try {
						performRemove(monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				});
			} catch (InvocationTargetException e) {
				Utils.handle(e);
			} catch (InterruptedException e) {
				// Ignore
			}
		}
	}

	private void performRemove(IProgressMonitor monitor) throws CoreException {
		IResource[] resources = getVisibleResources(monitor);
		if (resources.length == 0)
			return;
		ResourceDiffTree tree = (ResourceDiffTree)getSynchronizationContext().getDiffTree();
		try {
			tree.beginInput();
			for (IResource resource : resources) {
				tree.remove(resource);
			}
		} finally {
			tree.endInput(monitor);
		}
	}

	private IResource[] getVisibleResources(IProgressMonitor monitor) throws CoreException {
		ResourceTraversal[] traversals = getResourceTraversals(getStructuredSelection(), monitor);
		IDiff[] diffs = getSynchronizationContext().getDiffTree().getDiffs(traversals);
		List<IResource> result = new ArrayList<>();
		for (IDiff diff : diffs) {
			if (isVisible(diff)) {
				result.add(ResourceDiffTree.getResourceFor(diff));
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	private boolean confirmRemove() {
		IPreferenceStore store = TeamUIPlugin.getPlugin().getPreferenceStore();
		if (store.getBoolean(IPreferenceIds.SYNCVIEW_REMOVE_FROM_VIEW_NO_PROMPT)) {
			return true;
		} else {
			MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(
					getConfiguration().getSite().getShell(),
					TeamUIMessages.RemoveFromView_warningTitle,
					TeamUIMessages.RemoveFromView_warningMessage,
					TeamUIMessages.RemoveFromView_warningDontShow,
					false,
					null,
					null);
			store.setValue(IPreferenceIds.SYNCVIEW_REMOVE_FROM_VIEW_NO_PROMPT, dialog.getToggleState());
			return dialog.getReturnCode() == Window.OK;
		}
	}

	@Override
	protected boolean isEnabledForSelection(IStructuredSelection selection) {
		// Only enable if the selected elements adapt to IResource
		if (selection.isEmpty())
			return false;
		for (Object element : selection) {
			if (Utils.getResource(element) == null) {
				return false;
			}
		}
		return true;
	}
}
