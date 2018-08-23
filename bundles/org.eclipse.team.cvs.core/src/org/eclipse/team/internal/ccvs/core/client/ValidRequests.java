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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.CVSException;

class ValidRequests extends Request {
		
	protected ValidRequests() { }
	
	protected String getRequestId() {
		return "valid-requests"; //$NON-NLS-1$
	}

	public IStatus execute(Session session, IProgressMonitor monitor) throws CVSException {
		return executeRequest(session, Command.DEFAULT_OUTPUT_LISTENER, monitor);
	}
}
