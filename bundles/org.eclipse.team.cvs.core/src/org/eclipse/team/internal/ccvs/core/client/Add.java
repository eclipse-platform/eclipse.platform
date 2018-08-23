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


import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.MutableFolderSyncInfo;

public class Add extends Command {
	/*** Local options: specific to add ***/

	protected Add() { }
	protected String getRequestId() {
		return "add";  //$NON-NLS-1$
	}
	
	protected ICVSResource[] sendLocalResourceState(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor)
		throws CVSException {			

		// Check that all the arguments can give you an
		// repo that you will need while traversing the
		// file-structure
		for (int i = 0; i < resources.length; i++) {
			Assert.isNotNull(resources[i].getRemoteLocation(session.getLocalRoot()));
		}
		
		// Get a vistor and use it on every resource we should
		// work on
		AddStructureVisitor visitor = new AddStructureVisitor(session, localOptions);
		visitor.visit(session, resources, monitor);
		return resources;
	}
	
	/**
	 * If the add succeeded then folders have to be initialized with the 
	 * sync info
	 */
	protected IStatus commandFinished(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor,
		IStatus status) throws CVSException {
		
		if (status.getCode() == CVSStatus.SERVER_ERROR) {
			return status;
		}
				
		for (int i = 0; i < resources.length; i++) {
			if (resources[i].isFolder()) {
				ICVSFolder mFolder = (ICVSFolder) resources[i];
				FolderSyncInfo info = mFolder.getParent().getFolderSyncInfo();
				if (info == null) {
					status = mergeStatus(status, new CVSStatus(IStatus.ERROR, NLS.bind(CVSMessages.Add_invalidParent, new String[] { mFolder.getRelativePath(session.getLocalRoot()) }))); 
				} else {
					String repository = info.getRepository() + "/" + mFolder.getName();	 //$NON-NLS-1$
                    MutableFolderSyncInfo newInfo = info.cloneMutable();
                    newInfo.setRepository(repository);
					mFolder.setFolderSyncInfo(newInfo);
				}
			}
		}
		return status;
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.core.client.Command#getDefaultCommandOutputListener()
     */
    protected ICommandOutputListener getDefaultCommandOutputListener() {
        return new CommandOutputListener() {
            public IStatus errorLine(String line,
                    ICVSRepositoryLocation location, ICVSFolder commandRoot,
                    IProgressMonitor monitor) {
                
                String serverMessage = getServerMessage(line, location);
                if (serverMessage != null) {
                    if (serverMessage.indexOf("cvs commit") != -1 && serverMessage.indexOf("add") != -1 && serverMessage.indexOf("permanently") != -1) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        return OK;
                    if (serverMessage.startsWith("scheduling file") && serverMessage.indexOf("for addition") != -1) //$NON-NLS-1$ //$NON-NLS-2$
                        return OK;
                }
                return super.errorLine(line, location, commandRoot, monitor);
            }
        };
    }

}
