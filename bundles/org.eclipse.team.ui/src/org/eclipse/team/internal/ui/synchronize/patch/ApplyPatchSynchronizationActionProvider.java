/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.compare.internal.patch.HunkDiffNode;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ui.mapping.ResourceMarkAsMergedHandler;
import org.eclipse.team.internal.ui.mapping.ResourceMergeHandler;
import org.eclipse.team.ui.mapping.SynchronizationActionProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class ApplyPatchSynchronizationActionProvider extends
		SynchronizationActionProvider {

	public ApplyPatchSynchronizationActionProvider() {
		super();
	}

	@Override
	protected void initialize() {
		super.initialize();
		final ISynchronizePageConfiguration configuration = getSynchronizePageConfiguration();
		// Use custom handlers, disabled for hunks.
		registerHandler(MERGE_ACTION_ID, new ResourceMergeHandler(
				configuration, false) {
			@Override
			public void updateEnablement(IStructuredSelection selection) {
				super.updateEnablement(selection);
				// disable merge for hunks
				Object[] elements = getOperation().getElements();
				for (int i = 0; i < elements.length; i++) {
					if (elements[i] instanceof HunkDiffNode) {
						setEnabled(false);
						return;
					}
				}
			}
		});
		registerHandler(MARK_AS_MERGE_ACTION_ID,
				new ResourceMarkAsMergedHandler(configuration) {
			@Override
			public void updateEnablement(IStructuredSelection selection) {
				super.updateEnablement(selection);
				// disable mark as merged for hunks
				Object[] elements = getOperation().getElements();
				for (int i = 0; i < elements.length; i++) {
					if (elements[i] instanceof HunkDiffNode) {
						setEnabled(false);
						return;
					}
				}
			}
		});
		// 'Overwrite' action is not shown, see
		// ApplyPatchModelSynchronizeParticipant.ApplyPatchModelSynchronizeParticipantActionGroup.addToContextMenu(String,
		// Action, IMenuManager)
	}
}
