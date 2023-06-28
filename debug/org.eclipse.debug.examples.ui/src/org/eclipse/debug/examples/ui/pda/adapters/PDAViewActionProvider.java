/*******************************************************************************
 * Copyright (c) 2012, 2018 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial implementation
 *     IBM Corporation - bug fixing
 */
package org.eclipse.debug.examples.ui.pda.adapters;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewActionProvider;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;

/**
 * View action provider that returns a custom find action for the PDA debugger
 * in the variables view.
 * @since 3.8
 */
public class PDAViewActionProvider implements IViewActionProvider {

	Map<IPresentationContext, Action> fActions = new HashMap<>();

	@Override
	public IAction getAction(IPresentationContext presentationContext, String actionID) {
		if (presentationContext.getId().equals(IDebugUIConstants.ID_VARIABLE_VIEW) &&
			IDebugView.FIND_ACTION.equals(actionID) )
		{
			Action action = fActions.get(presentationContext);
			if (action == null) {
				action = new PDAVirtualFindAction(presentationContext);
				fActions.put(presentationContext, action);
			}
			return action;
		}
		return null;
	}
}
