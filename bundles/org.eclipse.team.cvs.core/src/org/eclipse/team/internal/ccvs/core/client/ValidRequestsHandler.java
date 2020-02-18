/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.client;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.CVSException;

/**
 * Handles a "Valid-requests" response from the CVS server.
 * <p>
 * Suppose as a result of performing a command the CVS server responds
 * as follows:
 * </p>
 * <pre>
 *   [...]
 *   Valid-requests ci co update Root Directory Valid-responses Argument ...\n
 *   [...]
 * </pre>
 * Then we remember the set of valid requests for this session in
 * preparation for isValidRequests queries.
 */
class ValidRequestsHandler extends ResponseHandler {
	public String getResponseID() {
		return "Valid-requests"; //$NON-NLS-1$
	}

	public void handle(Session session, String validRequests,
		IProgressMonitor monitor) throws CVSException {
		// remember the set of valid requests for this session
		session.setValidRequests(validRequests);
	}

}
