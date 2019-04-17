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
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;

public class CVSHistoryFilterAction extends Action {
	private TreeViewer viewer;
	private CVSHistoryFilter filter;
	private CVSHistoryPage page;

	public CVSHistoryFilterAction(CVSHistoryPage page) {
		this.page = page;
	}

	public void init(TreeViewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public void run() {
		CVSHistoryFilterDialog historyDialog = new CVSHistoryFilterDialog(viewer.getControl().getShell());
		if (filter != null) {
			historyDialog.setFilter(filter);
		}
		if (historyDialog.open() == Window.CANCEL) {
			return;
		}
		
		filter = historyDialog.getFilter();
		page.showFilter(filter);
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}
}
