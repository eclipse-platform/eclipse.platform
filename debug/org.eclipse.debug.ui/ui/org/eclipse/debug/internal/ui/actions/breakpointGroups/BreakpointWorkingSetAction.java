/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.breakpointGroups;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewPart;

/**
 * An action which clears (sets the null) the default breakpoint group.
 */
public abstract class BreakpointWorkingSetAction extends AbstractBreakpointsViewAction implements IPropertyChangeListener {

	protected IAction fAction;

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		update();
	}

	@Override
	public void init(IViewPart view) {
		super.init(view);
		DebugUIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}

	@Override
	public void dispose() {
		DebugUIPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		super.dispose();
	}
	@Override
	public void init(IAction action) {
		fAction = action;
		super.init(action);
		update();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(IInternalDebugUIConstants.MEMENTO_BREAKPOINT_WORKING_SET_NAME)) {
			update();
		}

	}
	protected abstract void update();
}
