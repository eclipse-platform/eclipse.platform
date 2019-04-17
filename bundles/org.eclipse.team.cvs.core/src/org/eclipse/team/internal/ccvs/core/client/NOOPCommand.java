/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
import org.eclipse.team.internal.ccvs.core.ICVSResource;

/**
 * Noop command that sends edit notifications to the server.
 */
public class NOOPCommand extends Command {

	@Override
	protected ICVSResource[] sendLocalResourceState(
		Session session,
		GlobalOption[] globalOptions,
		LocalOption[] localOptions,
		ICVSResource[] resources,
		IProgressMonitor monitor)
		throws CVSException {
		
		// The noop visitor will send any pending notifications
		new NOOPVisitor(session, localOptions).visit(session, resources, monitor);
		return resources;
	}

	@Override
	protected String getRequestId() {
		return "noop"; //$NON-NLS-1$
	}

	@Override
	protected void sendArguments(Session session, String[] arguments)throws CVSException {
		// don't send any arguments
	}
	
	@Override
	protected boolean isWorkspaceModification() {
		return false;
	}

}
