/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.compare.internal;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionDelegate;


public abstract class BaseCompareAction implements IActionDelegate {

	private ISelection fSelection;

	@Override
	final public void run(IAction action) {
		run(fSelection);
	}

	@Override
	final public void selectionChanged(IAction action, ISelection selection) {
		fSelection= selection;
		if (action != null)
			action.setEnabled(isEnabled(fSelection));
	}

	protected boolean isEnabled(ISelection selection) {
		return false;
	}

	abstract protected void run(ISelection selection);
}
