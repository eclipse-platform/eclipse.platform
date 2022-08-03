/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.ICompareNavigator;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.internal.CompareEditorInputNavigator;
import org.eclipse.compare.internal.patch.PatchFileDiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.ModelCompareEditorInput;
import org.eclipse.team.internal.ui.mapping.ResourceMarkAsMergedHandler;
import org.eclipse.team.internal.ui.mapping.ResourceMergeHandler;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;

public class ApplyPatchModelCompareEditorInput extends ModelCompareEditorInput {

	public ApplyPatchModelCompareEditorInput(
			ModelSynchronizeParticipant participant, ICompareInput input,
			IWorkbenchPage page,
			ISynchronizePageConfiguration synchronizeConfiguration) {
		super(participant, input, page, synchronizeConfiguration);
	}

	@Override
	protected void handleMenuAboutToShow(IMenuManager manager) {
		StructuredSelection selection = new StructuredSelection(((IResourceProvider)getCompareInput()).getResource());
		final ResourceMarkAsMergedHandler markAsMergedHandler = new ResourceMarkAsMergedHandler(getSynchronizeConfiguration());
		markAsMergedHandler.updateEnablement(selection);
		Action markAsMergedAction = new Action(TeamUIMessages.ModelCompareEditorInput_0) {
			@Override
			public void run() {
				try {
					markAsMergedHandler.execute(new ExecutionEvent());
				} catch (ExecutionException e) {
					TeamUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
			}

		};
		Utils.initAction(markAsMergedAction, "action.markAsMerged."); //$NON-NLS-1$
		markAsMergedAction.setEnabled(markAsMergedAction.isEnabled());

		final ResourceMergeHandler mergeHandler = new ResourceMergeHandler(getSynchronizeConfiguration(), false);
		mergeHandler.updateEnablement(selection);
		Action mergeAction = new Action(TeamUIMessages.ModelCompareEditorInput_1) {
			@Override
			public void run() {
				try {
					mergeHandler.execute(new ExecutionEvent());
				} catch (ExecutionException e) {
					TeamUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
			}
		};
		Utils.initAction(mergeAction, "action.merge."); //$NON-NLS-1$
		mergeAction.setEnabled(mergeAction.isEnabled());

		manager.insertAfter(IWorkbenchActionConstants.MB_ADDITIONS, new Separator("merge")); //$NON-NLS-1$
		manager.insertAfter("merge", markAsMergedAction); //$NON-NLS-1$
		manager.insertAfter("merge", mergeAction); //$NON-NLS-1$
	}

	@Override
	protected void contentsCreated() {
		super.contentsCreated();
		ICompareNavigator nav = getNavigator();
		if (nav instanceof CompareEditorInputNavigator) {
			final CompareEditorInputNavigator cein = (CompareEditorInputNavigator) nav;
			Object pane = cein.getPanes()[0]; // the structure input pane, top left
			if (pane instanceof CompareViewerPane) {
				CompareViewerPane cvp = (CompareViewerPane) pane;
				cvp.setSelection(StructuredSelection.EMPTY);
				cvp.addSelectionChangedListener(e -> feed1(cein));
				feed1(cein);
			}
		}
	}

	// see org.eclipse.compare.CompareEditorInput.feed1(ISelection)
	private void feed1(CompareEditorInputNavigator cein) {
		if (getCompareInput() instanceof PatchFileDiffNode) {
			Object pane = cein.getPanes()[1]; // the top middle pane
			if (pane instanceof CompareViewerPane) {
				CompareViewerPane cvp = (CompareViewerPane) pane;
				cvp.setInput(getCompareInput());
			}
			pane = cein.getPanes()[2]; // the top right pane
			if (pane instanceof CompareViewerPane) {
				CompareViewerPane cvp = (CompareViewerPane) pane;
				cvp.setInput(null); // clear downstream pane
			}
		}
	}
}
