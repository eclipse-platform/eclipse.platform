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

public class Log extends AbstractMessageCommand {
	/*** Local options: specific to log ***/
	
	public static LocalOption makeRevisionOption(String revision) {
		return new LocalOption("-r" + revision, null); //$NON-NLS-1$
	}
	public static final LocalOption RCS_FILE_NAMES_ONLY = new LocalOption("-R"); //$NON-NLS-1$
	
	public static final LocalOption NO_TAGS = new LocalOption("-N"); //$NON-NLS-1$
	
	protected Log() { }
	protected String getRequestId() {
		return "log"; //$NON-NLS-1$
	}
	
	protected ICVSResource[] sendLocalResourceState(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor)
		throws CVSException {			
		
		// Send all folders that are already managed to the server
		boolean sendEmptyFolders = Command.findOption(localOptions, RCS_FILE_NAMES_ONLY.getOption()) != null;
		new FileStructureVisitor(session, localOptions, sendEmptyFolders, false /* send modified contents */).visit(session, resources, monitor);
		return resources;
	}
}

