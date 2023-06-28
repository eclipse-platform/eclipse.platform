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

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.internal.core.groups.GroupLaunchConfigurationDelegate;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Handles the states whenever a group launch begins, and when one is done
 * launching.
 * <p>
 * The implementation uses some static variables to keep track as
 * IStatusHandlers are instantiated from scratch for each state change.
 *
 * @since 3.12
 */
public class GroupLaunchHandler implements IStatusHandler {

	private static AtomicInteger launchCounter = new AtomicInteger(0);
	private static boolean removeLaunchesState = false;

	@Override
	public Object handleStatus(IStatus status, Object source) throws CoreException {
		final IPreferenceStore prefStore = DebugUIPlugin.getDefault().getPreferenceStore();

		switch (status.getCode()) {
			case GroupLaunchConfigurationDelegate.CODE_GROUP_LAUNCH_START:
				int prevCount = launchCounter.getAndIncrement();
				if (prevCount == 0) {
					// Have to temporarily turn off the "remove terminated
					// launches when new one created" preference because it does
					// not work well for group launches
					removeLaunchesState = prefStore.getBoolean(IDebugUIConstants.PREF_AUTO_REMOVE_OLD_LAUNCHES);
					prefStore.setValue(IDebugUIConstants.PREF_AUTO_REMOVE_OLD_LAUNCHES, false);
				}
				break;
			case GroupLaunchConfigurationDelegate.CODE_GROUP_LAUNCH_DONE:
				int newCount = launchCounter.decrementAndGet();
				if (newCount == 0) {
					prefStore.setValue(IDebugUIConstants.PREF_AUTO_REMOVE_OLD_LAUNCHES, removeLaunchesState);
				}
				break;
			default:
				// unknown state...
				break;
		}
		return null;
	}

}
