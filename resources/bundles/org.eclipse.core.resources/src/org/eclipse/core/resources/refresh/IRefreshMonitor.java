/*******************************************************************************
 *  Copyright (c) 2004, 2009 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.resources.refresh;

import org.eclipse.core.resources.IResource;

/**
 * An <code>IRefreshMonitor</code> monitors trees of <code>IResources</code>
 * for changes in the local file system.
 * <p>
 * When an <code>IRefreshMonitor</code> notices changes, it should report them
 * to the <code>IRefreshResult</code> provided at the time of the monitor's
 * creation.
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @since 3.0
 */
public interface IRefreshMonitor {
	/**
	 * Informs the monitor that it should stop monitoring the given resource.
	 *
	 * @param resource the resource that should no longer be monitored, or
	 * <code>null</code> if this monitor should stop monitoring all resources
	 * it is currently monitoring
	 */
	void unmonitor(IResource resource);
}
