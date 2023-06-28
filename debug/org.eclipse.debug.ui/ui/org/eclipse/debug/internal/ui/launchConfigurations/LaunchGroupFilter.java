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
package org.eclipse.debug.internal.ui.launchConfigurations;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.activities.WorkbenchActivityHelper;

/**
 * Displays launch configurations for a specific launch group
 */
public class LaunchGroupFilter extends ViewerFilter {

	private ILaunchGroup fGroup;

	/**
	 * Constructor
	 * @param groupExtension
	 */
	public LaunchGroupFilter(ILaunchGroup groupExtension) {
		super();
		fGroup = groupExtension;
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
			ILaunchConfigurationType type = null;
			ILaunchConfiguration config = null;
			if (parentElement instanceof ILaunchConfigurationType) {
				type = (ILaunchConfigurationType)parentElement;
			}
			if (element instanceof ILaunchConfigurationType) {
				type = (ILaunchConfigurationType)element;
			}
			if (element instanceof ILaunchConfiguration) {
				config = (ILaunchConfiguration)element;
				try {
					type = config.getType();
				}
				catch (CoreException e) {}
			}
			boolean priv = false;
			if (config != null) {
				try {
					priv = config.getAttribute(IDebugUIConstants.ATTR_PRIVATE, false);
				} catch (CoreException e) {
				}
			}
			if (type != null) {
				return !priv && type.supportsMode(fGroup.getMode()) && equalCategories(type.getCategory(), fGroup.getCategory()) && !WorkbenchActivityHelper.filterItem(new LaunchConfigurationTypeContribution(type));
			}
			return false;
	}

	/**
	 * Returns whether the given categories are equal.
	 *
	 * @param c1 category identifier or <code>null</code>
	 * @param c2 category identifier or <code>null</code>
	 * @return boolean
	 */
	private boolean equalCategories(String c1, String c2) {
		if (c1 == null || c2 == null) {
			return c1 == c2;
		}
		return c1.equals(c2);
	}

}
