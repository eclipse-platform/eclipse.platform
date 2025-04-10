/*******************************************************************************
 *  Copyright (c) 2016 SSI Schaefer and others.
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
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.internal.ui.DebugUIPlugin;

/**
 * Handles additionally required actions when a member of a group has been
 * launched
 *
 * @since 3.12
 */
public class GroupElementLaunchedHandler implements IStatusHandler {

	@Override
	public Object handleStatus(IStatus status, Object source) throws CoreException {
		if (source instanceof ILaunch[]) {
			ILaunch[] launches = (ILaunch[]) source;

			// Now we need to override the history to make multi-launch
			// appear last, if we don't do it last launch would be our
			// child's launch which is not correct for repeating the
			// experience.
			DebugUIPlugin.getDefault().getLaunchConfigurationManager().setRecentLaunch(launches[0]);
		}
		return null;
	}

}
