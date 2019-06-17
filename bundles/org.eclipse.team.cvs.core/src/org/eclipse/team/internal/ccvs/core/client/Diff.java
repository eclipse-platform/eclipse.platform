/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;

/**
 * Runs the CVS diff command.
 */
public class Diff extends Command {
	/*** Local options: specific to diff ***/
	public static final LocalOption UNIFIED_FORMAT = new LocalOption("-u"); //$NON-NLS-1$
	public static final LocalOption CONTEXT_FORMAT = new LocalOption("-c"); //$NON-NLS-1$
	public static final LocalOption INCLUDE_NEWFILES = new LocalOption("-N"); //$NON-NLS-1$
	public static final LocalOption BRIEF = new LocalOption("--brief"); //$NON-NLS-1$

	protected Diff() { }
	protected String getRequestId() {
		return "diff"; //$NON-NLS-1$
	}
	
	/**
	 * Overwritten to throw the CVSDiffException if the server returns an error, because it just does 
	 * so when there is a difference between the checked files.	
	 */
	protected IStatus doExecute(Session session, GlobalOption[] globalOptions, LocalOption[] localOptions,
								String[] arguments, ICommandOutputListener listener, IProgressMonitor monitor) throws CVSException {
		try {
			IStatus status = super.doExecute(session, globalOptions, localOptions, arguments, listener, monitor);
			if (status.getCode() == CVSStatus.SERVER_ERROR) {
				if (status.isMultiStatus()) {
					IStatus[] children = status.getChildren();
					for (IStatus child : children) {
						if (child.getMessage().contains("[diff aborted]")) { //$NON-NLS-1$
							throw new CVSServerException(status);
						}
					}
				}
			}
			return status;
		} catch (CVSServerException e) {
			if (e.containsErrors()) throw e;
			return e.getStatus();
		}
	}

	protected ICVSResource[] sendLocalResourceState(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor)
		throws CVSException {			

		checkResourcesManaged(session, resources);
		DiffStructureVisitor visitor = new DiffStructureVisitor(session, localOptions);
		visitor.visit(session, resources, monitor);
		return resources;
	}
	
	protected String getServerErrorMessage() {
		return CVSMessages.Diff_serverError; 
	}
}
