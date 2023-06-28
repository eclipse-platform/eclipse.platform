/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.memory;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 * @since 3.1
 */
abstract public class ToggleViewPaneAction extends Action implements IViewActionDelegate, IActionDelegate2, IPropertyChangeListener {

	MemoryView fView;
	IAction fAction;

	@Override
	public void init(IViewPart view) {
		if (view instanceof MemoryView) {
			fView = (MemoryView) view;
		}
	}

	@Override
	public void run(IAction action) {

		if (fView == null) {
			return;
		}

		fView.showViewPane(!fView.isViewPaneVisible(getPaneId()), getPaneId());

		if (fView.isViewPaneVisible(getPaneId())) {
			action.setChecked(true);
		} else {
			action.setChecked(false);
		}

	}

	@Override
	public void run() {
		if (fView == null) {
			return;
		}

		fView.showViewPane(!fView.isViewPaneVisible(getPaneId()), getPaneId());
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (fView.isViewPaneVisible(getPaneId())) {
			action.setChecked(true);
		} else {
			action.setChecked(false);
		}
	}

	@Override
	public void dispose() {
		DebugUITools.getPreferenceStore().removePropertyChangeListener(this);
	}

	@Override
	public void init(IAction action) {
		fAction = action;
		DebugUITools.getPreferenceStore().addPropertyChangeListener(this);
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (fView != null && fAction != null) {
			if (fView.isViewPaneVisible(getPaneId())) {
				fAction.setChecked(true);
			} else {
				fAction.setChecked(false);
			}
		}
	}

	abstract public String getPaneId();
}
