/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers.model.provisional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * A progress monitor that accepts a status. This common base interface is used
 * to make non-blocking requests on models that may reply asynchronously. A request
 * may be cancelled by the caller or by the client fulfilling the request (by
 * calling <code>setCancelled(true)</code> on the request). Failure and error
 * states are reported by setting a status on a monitor. When a request
 * is complete, the client fulfilling the request must call <code>done()</code> on the
 * monitor, whether the update succeeds, fails, or is cancelled.
 * <p>
 * Clients accepting a status monitor are expected to poll the
 * monitor (using <code>isCanceled</code>) periodically and abort at their
 * earliest convenience if a request is cancelled.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @since 3.3
 * <p>
 * <strong>EXPERIMENTAL</strong>. This interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * remain unchanged during the 3.3 release cycle. Please do not use this API
 * without consulting with the Platform/Debug team.
 * </p>
 */
public interface IStatusMonitor extends IProgressMonitor {

	/**
	 * Sets the status for a request, possibly <code>null</code>.
	 * When a request fails, the status indicates why the request failed.
	 * A <code>null</code> status is considered to be successful.
	 *
	 * @param status request status
	 */
	void setStatus(IStatus status);

	/**
	 * Returns the status of this request, or <code>null</code>.
	 *
	 * @return request status - <code>null</code> is equivalent
	 *  to an OK status
	 */
	IStatus getStatus();
}
