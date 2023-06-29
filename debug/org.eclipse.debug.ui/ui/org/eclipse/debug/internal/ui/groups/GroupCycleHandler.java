/*******************************************************************************
 *  Copyright (c) 2016, 2018 SSI Schaefer and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      SSI Schaefer - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.groups;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

/**
 * Handles the case that a group is nested within itself.
 *
 * @since 3.12
 */
public class GroupCycleHandler implements IStatusHandler {

	@Override
	public Object handleStatus(IStatus status, final Object source) throws CoreException {
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), DebugUIMessages.GroupLaunch_Error, NLS.bind(DebugUIMessages.GroupLaunch_Cycle, source.toString())));
		return null;
	}

}
