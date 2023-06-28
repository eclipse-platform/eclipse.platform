/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 *
 * Provides implementation to filter unavailable projects form the launch configuration dialog.
 * It is (de) activated via the <code>IInternalDebugUIConstants.PREF_FILTER_LAUNCH_DELETED</code> preference.
 *
 * @since 3.2
 *
 */
public class DeletedProjectFilter extends ViewerFilter {

	/**
	 * Constructor
	 */
	public DeletedProjectFilter() {
		super();
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		//always let through types, we only care about configs
		if (element instanceof ILaunchConfigurationType) {
			return true;
		}
		if(element instanceof ILaunchConfiguration) {
			try {
				ILaunchConfiguration config = (ILaunchConfiguration)element;
				IResource[] resources = config.getMappedResources();
				if(resources == null) {
					return true;
				}
				for (IResource resource : resources) {
					IProject project = resource.getProject();
					if(project != null && project.exists()) {
						return true;
					}
				}
			}
			catch(CoreException e) {}
		}
		return false;
	}

}
