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

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.subscribers.SubscriberResourceMappingContext;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Perform a "cvs commit"
 */
public class CommitOperation extends SingleCommandOperation {

	public CommitOperation(IWorkbenchPart part, ResourceMapping[] mappers, LocalOption[] options, String comment) {
		super(part, mappers, options);
		addLocalOption(Command.makeArgumentOption(Command.MESSAGE_OPTION, comment));
	}
	
	@Override
	protected IStatus executeCommand(Session session, CVSTeamProvider provider, ICVSResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException {
		return Command.COMMIT.execute(session,
				Command.NO_GLOBAL_OPTIONS,
				getLocalOptions(recurse),
				resources, 
				null,
				monitor);
	}
	
	@Override
	protected String getTaskName() {
		return CVSUIMessages.RepositoryManager_committing; 
	}
	
	@Override
	protected String getTaskName(CVSTeamProvider provider) {
		return NLS.bind(CVSUIMessages.CommitOperation_0, new String[] { provider.getProject().getName() }); 
	}
	
	@Override
	protected String getErrorMessage(IStatus[] failures, int totalOperations) {
		return CVSUIMessages.CommitAction_commitFailed; 
	}
	
	@Override
	protected boolean isServerModificationOperation() {
		return true;
	}
	
	@Override
	protected ResourceMappingContext getResourceMappingContext() {
		return SubscriberResourceMappingContext.createContext(CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber());
	}
}
