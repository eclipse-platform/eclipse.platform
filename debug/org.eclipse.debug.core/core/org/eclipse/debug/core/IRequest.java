/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
package org.eclipse.debug.core;

import org.eclipse.core.runtime.IStatus;

/**
 * Common base interface for an asynchronously processed request. A request may succeed,
 * fail or be canceled as indicated by the request's status. When a request is complete
 * the client fulfilling the request must call <code>done()</code> on the request whether
 * the operation succeeds, fails, or is canceled.
 * <p>
 * Specific requests (sub types of this interface) often include data pertaining to
 * the request and usually contain results of the request.
 * </p>
 * <p>
 * Clients are expected to poll a request (using <code>isCanceled</code>)
 * periodically and abort at their earliest convenience calling <code>done()</code>.
 * A request can be canceled by the originator of the request or a client
 * fulfilling a request.
 * </p>
 * <p>
 * Clients that invoke request handlers may implement this interface.
 * </p>
 * @since 3.3
 */
public interface IRequest {

	/**
	 * Sets the status for this request indicating whether this request
	 * succeeded, failed, or was canceled. When a request fails, the status
	 * indicates why the request failed. A <code>null</code> status is considered
	 * to be successful. Only clients fulfilling a request should call this
	 * method. Clients making a request are not intended to call this method.
	 *
	 * @param status request status or <code>null</code>
	 */
	void setStatus(IStatus status);

	/**
	 * Returns the status of this request, or <code>null</code>.
	 *
	 * @return request status - <code>null</code> is equivalent
	 *  to an OK status
	 */
	IStatus getStatus();

	/**
	 * Indicates this request is complete. Clients must call this method
	 * whether the request succeeds, fails, or is cancelled to indicate that
	 * processing is complete. Only clients fulfilling a request should call this
	 * method. Clients making a request are not intended to call this method.
	 */
	void done();

	/**
	 * Cancels this request. A request may be canceled by the originator of request
	 * or a client fulfilling a request. Optionally a canceled status may be set on
	 * this request with more details. A client fulfilling a request must still call
	 * <code>done()</code> to indicate the request is complete.
	 */
	void cancel();

	/**
	 * Returns whether this request has been canceled.
	 * <p>
	 * Clients fulfilling a request are expected to poll a request (using <code>isCanceled</code>)
	 * periodically and abort at their earliest convenience calling <code>done()</code>.
	 * A request can be canceled by the originator of the request or a processor fulfilling a
	 * request.
	 * </p>
	 * @return whether this request has been canceled
	 */
	boolean isCanceled();

}
