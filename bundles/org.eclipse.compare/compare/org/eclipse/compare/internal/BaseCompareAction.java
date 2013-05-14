/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	final public void run(IAction action) {
		run(fSelection);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
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
