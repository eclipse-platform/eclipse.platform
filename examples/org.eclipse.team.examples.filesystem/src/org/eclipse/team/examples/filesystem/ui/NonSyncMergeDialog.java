/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.ui;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.ui.ISaveableWorkbenchPart;
import org.eclipse.team.ui.SaveablePartDialog;

/**
 * A dialog that can be used to merge conflicting model elements without
 * using the Synchronization framework. This is experimental.
 * See {@link NonSyncModelMergeOperation}
 * for a description of this work flow and its shortcomings.
 */
public class NonSyncMergeDialog extends SaveablePartDialog {

	public static void openFor(NonSyncModelMergeOperation operation) {
		NonSyncModelMergePage page = new NonSyncModelMergePage((IMergeContext) operation.getContext());
		NonSyncMergePart part = new NonSyncMergePart(operation.getShell(), new CompareConfiguration(), page);
		NonSyncMergeDialog dialog = new NonSyncMergeDialog(operation.getShell(), part);
		dialog.open();
	}

	public NonSyncMergeDialog(Shell shell, ISaveableWorkbenchPart input) {
		super(shell, input);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK) {
			NonSyncMergePart part = (NonSyncMergePart)getInput();
			IMergeContext context = part.getContext();
			if (hasUnmergedChanges(context)) {
				if (!MessageDialog.openQuestion(getShell(), "Unmerged Changes", "There are still unmerged changes. Are you sure you want to close the dialog?"))
					return;
			}
		}
		super.buttonPressed(buttonId);
	}

	private boolean hasUnmergedChanges(IMergeContext context) {
		return context.getDiffTree().hasMatchingDiffs(
				ResourcesPlugin.getWorkspace().getRoot().getFullPath(),
				new FastDiffFilter() {
					@Override
					public boolean select(IDiff diff) {
						if (diff instanceof IThreeWayDiff) {
							IThreeWayDiff twd = (IThreeWayDiff) diff;
							return twd.getDirection() == IThreeWayDiff.INCOMING || twd.getDirection() == IThreeWayDiff.CONFLICTING;
						}
						return false;
					}
				});
	}

}
