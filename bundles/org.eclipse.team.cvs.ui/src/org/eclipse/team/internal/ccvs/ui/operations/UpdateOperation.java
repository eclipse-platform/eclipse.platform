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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.subscribers.SubscriberResourceMappingContext;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Operation which performs a CVS update
 */
public class UpdateOperation extends SingleCommandOperation {

	private CVSTag tag;
	
	/**
	 * Create an UpdateOperation that will perform on update on the given resources
	 * using the given local option. If a tag is provided, it will be added to the 
	 * local options using the appropriate argument (-r or -D). If the tag is <code>null</code>
	 * then the tag will be omitted from the local options and the tags on the local resources
	 * will be used.
	 */
	public UpdateOperation(IWorkbenchPart part, IResource[] resources, LocalOption[] options, CVSTag tag) {
		this(part, asResourceMappers(resources), options, tag);
	}

    /**
     * Create an UpdateOperation that will perform on update on the given resources
     * using the given local option. If a tag is provided, it will be added to the 
     * local options using the appropriate argument (-r or -D). If the tag is <code>null</code>
     * then the tag will be omitted from the local options and the tags on the local resources
     * will be used.
     */
    public UpdateOperation(IWorkbenchPart part, ResourceMapping[] mappings, LocalOption[] options, CVSTag tag) {
        super(part, mappings, options);
        this.tag = tag;
    }

    /* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.SingleCommandOperation#executeCommand(org.eclipse.team.internal.ccvs.core.client.Session, org.eclipse.team.internal.ccvs.core.CVSTeamProvider, org.eclipse.core.resources.IResource[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus executeCommand(
		Session session,
		CVSTeamProvider provider,
		ICVSResource[] resources,
		boolean recurse, IProgressMonitor monitor)
		throws CVSException, InterruptedException {
			
			LocalOption[] commandOptions = getLocalOptions(recurse);
		
			monitor.beginTask(null, 100);
			IStatus execute = getUpdateCommand().execute(
				session,
				Command.NO_GLOBAL_OPTIONS, 
				commandOptions, 
				resources,
				getCommandOutputListener(),
				Policy.subMonitorFor(monitor, 95));
			
			updateWorkspaceSubscriber(provider, resources, recurse, Policy.subMonitorFor(monitor, 5));
			monitor.done();
			return execute;
	}

    protected LocalOption[] getLocalOptions(boolean recurse) {
        // Build the local options
        List localOptions = new ArrayList();
        // Use the appropriate tag options
        if (tag != null) {
        	localOptions.add(Update.makeTagOption(tag));
        }
        // Build the arguments list
        localOptions.addAll(Arrays.asList(super.getLocalOptions(recurse)));
        LocalOption[] commandOptions = (LocalOption[])localOptions.toArray(new LocalOption[localOptions.size()]);
        return commandOptions;
    }

	protected Update getUpdateCommand() {
		return Command.UPDATE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#getTaskName()
	 */
	protected String getTaskName() {
		return CVSUIMessages.UpdateOperation_taskName; //;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#getTaskName(org.eclipse.team.internal.ccvs.core.CVSTeamProvider)
	 */
	protected String getTaskName(CVSTeamProvider provider) {
		return NLS.bind(CVSUIMessages.UpdateOperation_0, new String[] { provider.getProject().getName() }); 
	}
	
	/**
	 * Return the listener that is used to process E and M messages.
	 * The default is <code>null</code>.
	 * @return
	 */
	protected ICommandOutputListener getCommandOutputListener() {
		return null;
	}
	
    protected boolean isReportableError(IStatus status) {
        return super.isReportableError(status)
        	|| status.getCode() == CVSStatus.UNMEGERED_BINARY_CONFLICT
        	|| status.getCode() == CVSStatus.INVALID_LOCAL_RESOURCE_PATH
        	|| status.getCode() == CVSStatus.RESPONSE_HANDLING_FAILURE;
    }

    /* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#getErrorMessage(org.eclipse.core.runtime.IStatus[], int)
	 */
	protected String getErrorMessage(IStatus[] failures, int totalOperations) {
		return CVSUIMessages.UpdateAction_update; 
	}
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#getResourceMappingContext()
     */
    protected ResourceMappingContext getResourceMappingContext() {
        return SubscriberResourceMappingContext.createContext(CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber());
    }

	public CVSTag getTag() {
		return tag;
	}

	public void setTag(CVSTag tag) {
		this.tag = tag;
	}
}
