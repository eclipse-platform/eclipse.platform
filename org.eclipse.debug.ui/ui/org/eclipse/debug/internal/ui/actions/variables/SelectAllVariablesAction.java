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
package org.eclipse.debug.internal.ui.actions.variables;

import org.eclipse.debug.internal.ui.actions.SelectAllAction;
import org.eclipse.debug.ui.IDebugView;

public class SelectAllVariablesAction extends SelectAllAction {


	@Override
	protected String getActionId() {
		return IDebugView.SELECT_ALL_ACTION + ".Variables"; //$NON-NLS-1$
	}

	@Override
	protected void initialize() {
	}

	@Override
	protected boolean isEnabled() {
		// TODO: only enable when stuff present
		return true;
	}
}
