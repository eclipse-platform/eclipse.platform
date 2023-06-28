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

import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointSetOrganizer;
import org.eclipse.jface.action.IAction;

/**
 * An action which clears (sets the null) the default breakpoint group.
 */
public class ClearDefaultBreakpointGroupAction extends BreakpointWorkingSetAction {

	@Override
	public void run(IAction action) {
		BreakpointSetOrganizer.setDefaultWorkingSet(null);
	}

	@Override
	protected void update() {
		fAction.setEnabled(BreakpointSetOrganizer.getDefaultWorkingSet() != null);
	}
}
