/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.history.*;

/**
 * Compare with revision will allow a user to browse the history of a file, compare with the
 * other revisions and merge changes from other revisions into the workspace copy.
 */
public class CompareWithRevisionAction extends WorkspaceAction {

	
	@Override
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
					
		// Show the compare viewer
		run((IRunnableWithProgress) monitor -> {
			if (isShowInDialog()) {
				IFile file = (IFile) getSelectedResources()[0];
				showCompareInDialog(getShell(), file);
			} else {
				IHistoryView view = TeamUI.showHistoryFor(TeamUIPlugin.getActivePage(), getSelectedResources()[0],
						null);
				IHistoryPage page = view.getHistoryPage();
				if (page instanceof CVSHistoryPage) {
					CVSHistoryPage cvsHistoryPage = (CVSHistoryPage) page;
					cvsHistoryPage.setClickAction(true);
				}
			}
		}, false /* cancelable */, PROGRESS_BUSYCURSOR);
	}
	
	protected void showCompareInDialog(Shell shell, Object object){
		IHistoryPageSource pageSource = HistoryPageSource.getHistoryPageSource(object);
		if (pageSource != null && pageSource.canShowHistoryFor(object)) {
			CompareConfiguration cc = new CompareConfiguration();
			cc.setLeftEditable(true);
			cc.setRightEditable(false);
			HistoryPageCompareEditorInput input = new HistoryPageCompareEditorInput(cc, pageSource, object) {
				@Override
				public void saveChanges(IProgressMonitor monitor) throws CoreException {
					super.saveChanges(monitor);
					((CVSHistoryPage)getHistoryPage()).saveChanges(monitor);
					setDirty(false);
				}
			};
			CompareUI.openCompareDialog(input);
		}
	}
	
	/**
	 * Return the text describing this action
	 */
	protected String getActionTitle() {
		return CVSUIMessages.CompareWithRevisionAction_4; 
	}
	
	@Override
	protected String getErrorTitle() {
		return CVSUIMessages.CompareWithRevisionAction_compare; 
	}

	@Override
	protected boolean isEnabledForCVSResource(ICVSResource cvsResource) throws CVSException {
		return (!cvsResource.isFolder() && super.isEnabledForCVSResource(cvsResource));
	}

	@Override
	protected boolean isEnabledForMultipleResources() {
		return false;
	}

	@Override
	protected boolean isEnabledForAddedResources() {
		return true;
	}
	
	@Override
	protected boolean isEnabledForUnmanagedResources() {
		return true;
	}
	
	@Override
	protected boolean isEnabledForIgnoredResources() {
		return true;
	}

	protected boolean isShowInDialog() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_SHOW_COMPARE_REVISION_IN_DIALOG);
	}
}
