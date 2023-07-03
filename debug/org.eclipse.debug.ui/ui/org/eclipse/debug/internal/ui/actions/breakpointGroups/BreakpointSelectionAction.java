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
package org.eclipse.debug.internal.ui.actions.breakpointGroups;

import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsView;
import org.eclipse.ui.actions.SelectionListenerAction;

/**
 * A selection listener action for the breakpoints view.
 */
public abstract class BreakpointSelectionAction extends SelectionListenerAction {

	private BreakpointsView fView;

	/**
	 * Constructs an action for the breakpoints view.
	 *
	 * @param text action name
	 * @param view breakpoints view
	 */
	public BreakpointSelectionAction(String text, BreakpointsView view) {
		super(text);
		fView = view;
	}

	/**
	 * Returns the breakpoints view.
	 *
	 * @return breakpoints view
	 */
	protected BreakpointsView getBreakpointsView() {
		return fView;
	}

}
