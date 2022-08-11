/*******************************************************************************
 * Copyright (c) 2004, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.resources.refresh.win32;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.refresh.*;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The <code>Win32RefreshProvider</code> creates monitors that
 * can monitor drives on Win32 platforms.
 *
 * @see RefreshProvider
 */
public class Win32RefreshProvider extends RefreshProvider {
	private Win32Monitor monitor;

	/**
	 * Creates a standard Win32 monitor if the given resource is local.
	 *
	 * @see RefreshProvider#installMonitor(IResource,IRefreshResult, IProgressMonitor)
	 */
	@Override
	public IRefreshMonitor installMonitor(IResource resource, IRefreshResult result, IProgressMonitor progressMonitor) {
		if (resource.getLocation() == null || !resource.exists() || resource.getType() == IResource.FILE)
			return null;
		if (monitor == null)
			monitor = new Win32Monitor(result);
		if (monitor.monitor(resource))
			return monitor;
		return null;
	}
}
