/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools;

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;

public class SelectAllAction extends GlobalAction {

	private ITextOperationTarget target;

	public SelectAllAction(ITextOperationTarget target) {
		super("Select &All"); //$NON-NLS-1$
		this.target = target;
	}

	/**
	 * Registers this action as a global action handler.
	 *
	 * @param actionBars the action bars where this action will be registered.
	 * @see org.eclipse.core.tools.GlobalAction#registerAsGlobalAction(org.eclipse.ui.IActionBars)
	 */
	@Override
	public void registerAsGlobalAction(IActionBars actionBars) {
		actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), this);
	}

	@Override
	public void run() {
		target.doOperation(ITextOperationTarget.SELECT_ALL);
	}

}
