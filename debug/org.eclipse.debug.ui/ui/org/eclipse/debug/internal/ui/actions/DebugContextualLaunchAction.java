/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.actions.ContextualLaunchAction;

/**
 * Specialization of <code>ContextualLaunchAction</code> for the debug mode
 *
 * @see {@link ContextualLaunchAction}
 */
public class DebugContextualLaunchAction extends ContextualLaunchAction {

	/**
	 * Constructor
	 */
	public DebugContextualLaunchAction() {
		super(ILaunchManager.DEBUG_MODE);
	}
}
