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
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSResource;

public class SyncUpdate extends Update {

	public SyncUpdate() { };

	/*
	 * @see Command#sendFileStructure(ICVSResource,IProgressMonitor,boolean,boolean,boolean)
	 */
    protected void sendFileStructure(Session session, ICVSResource[] resources,
            LocalOption[] localOptions, boolean emptyFolders, IProgressMonitor monitor) throws CVSException {
			
		checkResourcesManaged(session, resources);
		new FileStructureVisitor(session, localOptions, emptyFolders, true, false).visit(session, resources, monitor);
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.core.client.Command#isWorkspaceModification()
     */
    protected boolean isWorkspaceModification() {
        // The sync-update will not modify the workspace
        return false;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.core.client.Command#filterGlobalOptions(org.eclipse.team.internal.ccvs.core.client.Session, org.eclipse.team.internal.ccvs.core.client.Command.GlobalOption[])
     */
    protected GlobalOption[] filterGlobalOptions(Session session, GlobalOption[] globalOptions) {
        // Ensure that the DO_NOT_CHANGE (-n) global option is present
		if (! Command.DO_NOT_CHANGE.isElementOf(globalOptions)) {
			globalOptions = Command.DO_NOT_CHANGE.addToEnd(globalOptions);
		}
        return super.filterGlobalOptions(session, globalOptions);
    }

}
