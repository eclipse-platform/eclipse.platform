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
package org.eclipse.debug.internal.ui.actions.breakpoints;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IWatchpoint;

/**
 * Toggles access attribute of a watchpoint.
 */
public class ModificationWatchpointToggleAction extends ModifyWatchpointAction {

	@Override
	protected boolean isEnabled(IWatchpoint watchpoint) {
		return watchpoint.supportsModification();
	}

	@Override
	protected void toggleWatchpoint(IWatchpoint watchpoint, boolean b) throws CoreException {
		watchpoint.setModification(b);
	}

	@Override
	protected boolean isChecked(IWatchpoint watchpoint) {
		try {
			return watchpoint.isModification();
		} catch (CoreException e) {
		}
		return false;
	}

}
