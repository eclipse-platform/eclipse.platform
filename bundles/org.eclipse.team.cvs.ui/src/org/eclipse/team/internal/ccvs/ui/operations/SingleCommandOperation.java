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
package org.eclipse.team.internal.ccvs.ui.operations;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.IWorkbenchPart;

public abstract class SingleCommandOperation extends RepositoryProviderOperation {
	
	private LocalOption[] options = Command.NO_LOCAL_OPTIONS;
	
	public SingleCommandOperation(IWorkbenchPart part, ResourceMapping[] mappings, LocalOption[] options) {
		super(part, mappings);
		if (options != null) {
			this.options = options;
		}
	}

	@Override
	protected void execute(CVSTeamProvider provider, IResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException {
		monitor.beginTask(null, 100);
		Session session = new Session(getRemoteLocation(provider), getLocalRoot(provider), true /* output to console */);
		session.open(Policy.subMonitorFor(monitor, 10), isServerModificationOperation());
		try {
			IStatus status = executeCommand(session, provider, getCVSArguments(session, resources), recurse, Policy.subMonitorFor(monitor, 90));
			if (isReportableError(status)) {
				throw new CVSException(status);
			}
		} finally {
			session.close();
		}
	}

	@Override
	protected final ICVSResource[] getCVSArguments(IResource[] resources) {
		return super.getCVSArguments(resources);
	}
		
	protected ICVSResource[] getCVSArguments(Session session, IResource[] resources) {
		return getCVSArguments(resources);
	}

	@Override
	protected void execute(CVSTeamProvider provider, ICVSTraversal entry, IProgressMonitor monitor) throws CVSException, InterruptedException {
		try {
			// TODO: This does not properly count the number of operations
			// Changing it causes an error in the test cases
			super.execute(provider, entry, monitor);
			collectStatus(Status.OK_STATUS);
		} catch (CVSException e) {
			collectStatus(e.getStatus());
		}
	}
	/**
	 * Indicate whether the operation requires write access to the server (i.e.
	 * the operation changes state on the server whether it be to commit, tag, admin, etc).
	 * @return
	 */
	protected boolean isServerModificationOperation() {
		return false;
	}

	/**
	 * Method overridden by subclasses to issue the command to the CVS repository using the given session.
	 * @param session an open session which will be closed by the caller
	 * @param provider the provider for the project that contains the resources
	 * @param resources the resources to be operated on
	 * @param recurse whether the operation is deep or shallow
	 * @param monitor a progress monitor
	 */
	protected abstract IStatus executeCommand(Session session, CVSTeamProvider provider, ICVSResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException;

	@Override
	protected LocalOption[] getLocalOptions(boolean recurse) {
		LocalOption[] result = options;
		if (recurse) {
			// For deep operations, we just need to make sure that the -l option isn't present
			result = Command.DO_NOT_RECURSE.removeFrom(options);
		} else {
			result = Command.RECURSE.removeFrom(options);
			result = Command.DO_NOT_RECURSE.addTo(options);
		}
		return result;
	}

	protected void setLocalOptions(LocalOption[] options) {
		this.options = options;
	}

	protected void addLocalOption(LocalOption option) {
		options = option.addTo(options);
	}
}
