/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.history;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.history.HistoryPageCompareEditorInput;
import org.eclipse.team.ui.history.IHistoryPageSource;

public class ReplaceLocalHistory extends ShowLocalHistory {

	@Override
	public void run(IAction action) {
		final IFile file = (IFile) getSelection().getFirstElement();
		IFileState states[]= getLocalHistory();
		if (states == null || states.length == 0)
			return;
		Runnable r = () -> showCompareInDialog(getShell(), file);
		TeamUIPlugin.getStandardDisplay().asyncExec(r);
	}

	private void showCompareInDialog(Shell shell, Object object){
		IHistoryPageSource pageSource = LocalHistoryPageSource.getInstance();
		if (pageSource != null && pageSource.canShowHistoryFor(object)) {
			CompareConfiguration cc = new CompareConfiguration();
			cc.setLeftEditable(false);
			cc.setRightEditable(false);
			HistoryPageCompareEditorInput input = new HistoryPageCompareEditorInput(cc, pageSource, object) {
				@Override
				public boolean isEditionSelectionDialog() {
					return true;
				}
				@Override
				public String getOKButtonLabel() {
					return TeamUIMessages.ReplaceLocalHistory_0;
				}
				@Override
				public boolean okPressed() {
					try {
						Object o = getSelectedEdition();
						FileRevisionTypedElement right = (FileRevisionTypedElement) ((ICompareInput)o).getRight();
						IFile file = (IFile)getCompareResult();
						file.setContents(right.getContents(), false, true, null);
					} catch (CoreException e) {
						Utils.handle(e);
						return false;
					}
					return true;
				}
			};
			CompareUI.openCompareDialog(input);
		}
	}

	@Override
	protected String getPromptTitle() {
		return TeamUIMessages.ReplaceLocalHistory_1;
	}
}
