/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.ui.actions.ILaunchable;

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
				if (DebugUIPlugin.getDefault().getLaunchConfigurationManager().launchModeAvailable((String)expectedValue)) {
					return Platform.getAdapterManager().hasAdapter(receiver, ILaunchable.class.getName());
				}
		}
		if("contextlaunch".equals(property)) { //$NON-NLS-1$
			if(DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_USE_CONTEXTUAL_LAUNCH)) {
				IResource res = getResource(receiver);
				if(res != null) {
					return res.isAccessible() && getLaunchConfigurationManager().getLaunchShortcuts(getResource(receiver)).size() > 0 && getLaunchConfigurationManager().isSharedConfig(receiver) == null;
				}
				return false;
			}
		}
		return false;
	}

	/**
	 * Returns the launch configuration manager
	 * @return the launch configuration manager
	 */
	protected LaunchConfigurationManager getLaunchConfigurationManager() {
		return DebugUIPlugin.getDefault().getLaunchConfigurationManager();
	}

	/**
	 * Returns the resource this property page is open on.
	 *
	 * @return resource
	 */
	protected IResource getResource(Object element) {
		IResource resource = null;
		if (element instanceof IResource) {
			resource = (IResource) element;
		} else if (element instanceof IAdaptable) {
			resource = ((IAdaptable)element).getAdapter(IResource.class);
		}
		return resource;
	}
}
