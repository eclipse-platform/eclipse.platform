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

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IActionBars;

/**
 * An abstract base class for global actions. Global actions
 * are able to register themselves in a view's action bars.
 */
public abstract class GlobalAction extends Action {
	/**
	 * Constructs an action.
	 *
	 * @param text the action description
	 */
	GlobalAction(String text) {
		super(text);
		setToolTipText(text);
	}

	/**
	 * Registers this action as a global action handler.
	 *
	 * @param actionBars the action bars where this action will be registered.
	 * @see IActionBars#updateActionBars()
	 */
	public abstract void registerAsGlobalAction(IActionBars actionBars);
}
