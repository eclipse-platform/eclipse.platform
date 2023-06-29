/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.breakpoints;

import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsView;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 * An action which toggles the "Link with Debug View" preference on a
 * breakpoints view.
 */
public class LinkBreakpointsWithDebugViewAction implements IViewActionDelegate {

	private BreakpointsView fView;
	private IAction fAction= null;

	public LinkBreakpointsWithDebugViewAction() {
	}

	@Override
	public void init(IViewPart view) {
		fView= (BreakpointsView) view;
	}

	@Override
	public void run(IAction action) {
		fView.setTrackSelection(action.isChecked());
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (fAction == null) {
			action.setChecked(fView.isTrackingSelection());
			fAction= action;
		}
	}

}
