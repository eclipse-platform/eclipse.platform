/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.core;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;

/**
 * Tests if an object is launchable.
 */
public class LaunchablePropertyTester extends PropertyTester {

	/**
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if ("launchable".equals(property)) { //$NON-NLS-1$
				if (((LaunchManager)(DebugPlugin.getDefault().getLaunchManager())).launchModeAvailable((String)expectedValue)) {
					return Platform.getAdapterManager().hasAdapter(receiver, "org.eclipse.debug.ui.actions.ILaunchable"); //$NON-NLS-1$
				}
		}
		return false;
	}

}
