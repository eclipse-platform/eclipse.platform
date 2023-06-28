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
 * This implementation is used to filter closed projects from the launch configuration dialog.
 * It is (de)activated via the <code>IInternalDebugUIConstants.PREF_FILTER_LAUNCH_CLOSED</code> preference, and is
 * provided to fix bug 19521.
 *
 * @since 3.2
 *
 */
public class ClosedProjectFilter extends ViewerFilter {

	/**
	 * Constructor
	 */
	public ClosedProjectFilter() {
		super();
	}

	@Override
	public boolean select(Viewer viewer, Object parent, Object element) {
		//always let through types, we only care about configs
		if(element instanceof ILaunchConfigurationType) {
			return true;
		}
		if(element instanceof ILaunchConfiguration) {
			try {
				ILaunchConfiguration config = (ILaunchConfiguration)element;
				IResource[] resources = config.getMappedResources();
				//if it has no mapping, it might not have migration delegate, so let it pass
				if(resources == null) {
					return true;
				}
				for (IResource resource : resources) {
					IProject project = resource.getProject();
					//we don't want overlap with the deleted projects filter, so we need to allow projects that don't exist through
					if(project != null && (project.isOpen() || !project.exists())) {
						return true;
					}
				}
			}
			catch (CoreException e) {}
		}
		return false;
	}
}
