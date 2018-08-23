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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;
import org.eclipse.team.internal.ccvs.core.client.listeners.TagListener;

public class Tag extends Command {
	/*** Local options: specific to tag ***/
	public static final LocalOption CREATE_BRANCH = new LocalOption("-b", null);	 //$NON-NLS-1$	
	public static final LocalOption FORCE_REASSIGNMENT = new LocalOption("-F", null); //$NON-NLS-1$	

	/*** Default command output listener ***/
	private static final ICommandOutputListener DEFAULT_OUTPUT_LISTENER = new TagListener();
	
	// handle added and removed resources in a special way
	private boolean customBehaviorEnabled;
	
	protected Tag(boolean customBehaviorEnabled) {
		this.customBehaviorEnabled = customBehaviorEnabled;
	}
	
	protected Tag() {
		this(false);
	}
	
	protected String getRequestId() {
		return "tag"; //$NON-NLS-1$
	}

	protected ICVSResource[] computeWorkResources(Session session, LocalOption[] localOptions,
		String[] arguments) throws CVSException {
			
		if (arguments.length < 1) throw new IllegalArgumentException();
		String[] allButFirst = new String[arguments.length - 1];
		System.arraycopy(arguments, 1, allButFirst, 0, arguments.length - 1);
		return super.computeWorkResources(session, localOptions, allButFirst);
	}

	public IStatus execute(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, CVSTag tag, String[] arguments, ICommandOutputListener listener,
		IProgressMonitor monitor) throws CVSException {
		
		if(tag.getType() != CVSTag.VERSION && tag.getType() != CVSTag.BRANCH) {
			throw new CVSException(new CVSStatus(IStatus.ERROR, CVSMessages.Tag_notVersionOrBranchError)); 
		}
		
		// Add the CREATE_BRANCH option for a branch tag
		if (tag.getType() == CVSTag.BRANCH) {
			if ( ! CREATE_BRANCH.isElementOf(localOptions)) {
				LocalOption[] newLocalOptions = new LocalOption[localOptions.length + 1];
				System.arraycopy(localOptions, 0, newLocalOptions, 0, localOptions.length);
				newLocalOptions[newLocalOptions.length - 1] = CREATE_BRANCH;
				localOptions = newLocalOptions;
			}
		}
		
		// Add the tag name to the start of the arguments
		String[] newArguments = new String[arguments.length + 1];
		newArguments[0] = tag.getName();
		System.arraycopy(arguments, 0, newArguments, 1, arguments.length);
		
		return execute(session, globalOptions, localOptions, newArguments, listener, monitor);	
	}

	public IStatus execute(Session session, GlobalOption[] globalOptions, LocalOption[] localOptions, 
		CVSTag tag, ICVSResource[] arguments, ICommandOutputListener listener, IProgressMonitor monitor) 
		throws CVSException {
		
		String[] stringArguments = convertArgumentsForOpenSession(arguments, session);

		return execute(session, globalOptions, localOptions, tag, stringArguments, listener, monitor);
	}
	
	protected ICommandOutputListener getDefaultCommandOutputListener() {
		return DEFAULT_OUTPUT_LISTENER;
	}
		
	protected ICVSResource[] sendLocalResourceState(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor)
		throws CVSException {			

		// Send all folders that are already managed to the server
		if (customBehaviorEnabled) {
			new TagFileSender(session, localOptions).visit(session, resources, monitor);
		} else {
			new FileStructureVisitor(session, localOptions, false, false).visit(session, resources, monitor);
		}
		return resources;
	}
}
