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
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.ui.TeamUI;

public class ShowHistoryAction extends CVSAction {
	/**
	 * Returns the selected remote files
	 */
	protected ICVSRemoteFile[] getSelectedRemoteFiles() {
		ArrayList<Object> resources = null;
		IStructuredSelection selection = getSelection();
		if (!selection.isEmpty()) {
			resources = new ArrayList<>();
			Iterator elements = selection.iterator();
			while (elements.hasNext()) {
				Object next = elements.next();
				if (next instanceof ICVSRemoteFile) {
					resources.add(next);
					continue;
				}
				if (next instanceof IAdaptable) {
					IAdaptable a = (IAdaptable)next;
					Object adapter = a.getAdapter(ICVSRemoteFile.class);
					if (adapter instanceof ICVSRemoteFile) {
						resources.add(adapter);
						continue;
					}
				}
			}
		}
		if (resources != null && !resources.isEmpty()) {
			ICVSRemoteFile[] result = new ICVSRemoteFile[resources.size()];
			resources.toArray(result);
			return result;
		}
		return new ICVSRemoteFile[0];
	}

	@Override
	public void execute(IAction action) throws InterruptedException, InvocationTargetException {
		run((IRunnableWithProgress) monitor -> {
			ICVSRemoteFile[] files = getSelectedRemoteFiles();
			TeamUI.showHistoryFor(getTargetPage(), files[0], null);
		}, false /* cancelable */, PROGRESS_BUSYCURSOR);
	}

	@Override
	public boolean isEnabled() {
		ICVSRemoteFile[] resources = getSelectedRemoteFiles();
		return resources.length == 1;
	}

	@Override
	protected String getErrorTitle() {
		return CVSUIMessages.ShowHistoryAction_showHistory; 
	}

}
