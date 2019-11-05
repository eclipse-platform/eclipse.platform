/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import java.util.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;
import org.eclipse.team.internal.ccvs.core.client.listeners.TagListener;

public class RTag extends RemoteCommand {
	/*** Local options: specific to tag ***/
	public static final LocalOption CREATE_BRANCH = Tag.CREATE_BRANCH;
	public static final LocalOption CLEAR_FROM_REMOVED = new LocalOption("-a", null); //$NON-NLS-1$	
	public static final LocalOption FORCE_REASSIGNMENT = new LocalOption("-F", null); //$NON-NLS-1$
	public static final LocalOption FORCE_BRANCH_REASSIGNMENT = new LocalOption("-B", null); //$NON-NLS-1$

	/*** Default command output listener ***/
	private static final ICommandOutputListener DEFAULT_OUTPUT_LISTENER = new TagListener();
	
	/**
	 * Makes a -r or -D option for a tag.
	 * Valid for: checkout export history rdiff update
	 */
	public static LocalOption makeTagOption(CVSTag tag) {
		int type = tag.getType();
		switch (type) {
			case CVSTag.BRANCH:
			case CVSTag.VERSION:
			case CVSTag.HEAD:
				return new LocalOption("-r", tag.getName()); //$NON-NLS-1$
			case CVSTag.DATE:
				return new LocalOption("-D", tag.getName()); //$NON-NLS-1$
			default:
				// Unknow tag type!!!
				throw new IllegalArgumentException();
		}
	}
	
	protected String getRequestId() {
		return "rtag"; //$NON-NLS-1$
	}

	protected ICVSResource[] computeWorkResources(Session session, LocalOption[] localOptions,
		String[] arguments) throws CVSException {
		if (arguments.length < 2) throw new IllegalArgumentException();
		return super.computeWorkResources(session, localOptions, arguments);
	}
	
	public IStatus execute(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, CVSTag sourceTag, CVSTag tag, String[] arguments,
		IProgressMonitor monitor) throws CVSException {
		
		if(tag.getType() != CVSTag.VERSION && tag.getType() != CVSTag.BRANCH) {
			throw new CVSException(new CVSStatus(IStatus.ERROR, CVSMessages.Tag_notVersionOrBranchError)); 
		}
		
		// Add the source tag to the local options
		List<LocalOption> modifiedLocalOptions = new ArrayList<>(localOptions.length + 1);
		if (sourceTag==null) sourceTag = CVSTag.DEFAULT;
		modifiedLocalOptions.addAll(Arrays.asList(localOptions));
		modifiedLocalOptions.add(makeTagOption(sourceTag));
		
		// Add the CREATE_BRANCH option for a branch tag
		if (tag.getType() == CVSTag.BRANCH) {
			if ( ! CREATE_BRANCH.isElementOf(localOptions)) {
				modifiedLocalOptions.add(CREATE_BRANCH);
			}
		}
		
		// Add the tag name to the start of the arguments
		String[] newArguments = new String[arguments.length + 1];
		newArguments[0] = tag.getName();
		System.arraycopy(arguments, 0, newArguments, 1, arguments.length);
		
		return execute(session, globalOptions, 
			modifiedLocalOptions.toArray(new LocalOption[modifiedLocalOptions.size()]), 
			newArguments, null, monitor);
	}
	
	public IStatus execute(Session session, GlobalOption[] globalOptions, LocalOption[] localOptions, 
		CVSTag sourceTag, CVSTag tag, ICVSRemoteResource[] arguments, IProgressMonitor monitor) 
		throws CVSException {
		
		String[] stringArguments = convertArgumentsForOpenSession(arguments, session);

		return execute(session, globalOptions, localOptions, sourceTag, tag, stringArguments, monitor);
	}
	
	protected ICommandOutputListener getDefaultCommandOutputListener() {
		return DEFAULT_OUTPUT_LISTENER;
	}
}
