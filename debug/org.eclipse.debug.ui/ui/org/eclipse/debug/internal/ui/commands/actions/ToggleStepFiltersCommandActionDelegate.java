/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.commands.actions;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IAction;

/**
 * Toggle step filters action delegate.
 *
 * @since 3.3
 */
public class ToggleStepFiltersCommandActionDelegate extends DebugCommandActionDelegate {

	/**
	 * Constructor
	 */
	public ToggleStepFiltersCommandActionDelegate() {
		super();
		setAction(new ToggleStepFiltersAction());
	}

	@Override
	public void init(IAction action) {
		super.init(action);
		action.setChecked(DebugUITools.isUseStepFilters());
	}
}
