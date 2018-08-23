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
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.operations.BranchOperation;

/**
 * BranchAction tags the selected resources with a branch tag specified by the user,
 * and optionally updates the local resources to point to the new branch.
 */
public class BranchAction extends WorkspaceTraversalAction {

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#execute(org.eclipse.jface.action.IAction)
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		ResourceMapping[] resourceMappings = getCVSResourceMappings();
        if (resourceMappings == null || resourceMappings.length == 0) {
            // Could be a sync element tat is selected
            IResource[] resources = getSelectedResources();
            resourceMappings = getResourceMappings(resources);
        }
        if (resourceMappings == null || resourceMappings.length == 0) {
            // Nothing is select so just return
            return;
        }
        new BranchOperation(getTargetPart(), resourceMappings).run();
	}
	
	private ResourceMapping[] getResourceMappings(IResource[] resources) {
        List mappings = new ArrayList();
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            Object o = getAdapter(resource, ResourceMapping.class);
            if (o instanceof ResourceMapping) {
                ResourceMapping mapping = (ResourceMapping) o;
                mappings.add(mapping);
            }
        }
        return (ResourceMapping[]) mappings.toArray(new ResourceMapping[mappings.size()]);
    }

    /* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getId()
	 */
	public String getId() {
		return ICVSUIConstants.CMD_BRANCH;
	}
}

