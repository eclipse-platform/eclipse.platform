/*******************************************************************************
 *  Copyright (c) 2019 ArSysOp and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      Alexander Fedorov <alexander.fedorov@arsysop.ru> - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.preference.IPreferenceStore;

public class BuildBeforeLaunchStatusHandler implements IStatusHandler {

	@Override
	public Object handleStatus(IStatus status, Object source) throws CoreException {
		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
		return Boolean.parseBoolean(store.getString(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH));
	}

}
