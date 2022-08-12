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
 * An <code>IRefreshResult</code> is provided to an auto-refresh
 * monitor.  The result is used to submit resources to be refreshed, and
 * for reporting failure of the monitor.
 *
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IRefreshResult {
	/**
	 * Notifies that the given monitor has encountered a failure from which it
	 * cannot recover while monitoring the given resource.
	 * <p>
	 * If the given resource is <code>null</code> it indicates that the
	 * monitor has failed completely, and the refresh manager will have to
	 * take over the monitoring responsibilities for all resources that the
	 * monitor was monitoring.
	 *
	 * @param monitor a monitor which has encountered a failure that it
	 * cannot recover from
	 * @param resource the resource that the monitor can no longer
	 * monitor, or <code>null</code> to indicate that the monitor can no
	 * longer monitor any of the resources it was monitoring
	 */
	void monitorFailed(IRefreshMonitor monitor, IResource resource);

	/**
	 * Requests that the provided resource be refreshed.  The refresh will
	 * occur in the background during the next scheduled refresh.
	 *
	 * @param resource the resource to refresh
	 */
	void refresh(IResource resource);
}
