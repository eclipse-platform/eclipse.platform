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
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.subscribers.SubscriberResourceMappingContext;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.wizards.GenerateDiffFileWizard;
import org.eclipse.ui.PlatformUI;

/**
 * Action to generate a patch file using the CVS diff command.
 * 
 * NOTE: This is a temporary action and should eventually be replaced
 * by a create patch command in the compare viewer.
 */
public class GenerateDiffFileAction extends WorkspaceTraversalAction{
    
	/** (Non-javadoc)
	 * Method declared on IActionDelegate.
	 */
	@Override
	public void execute(IAction action) {

		try {
			final IResource [][] resources = new IResource[][] { null };
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
				try {
					resources[0] = getDeepResourcesToPatch(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			});
			GenerateDiffFileWizard.run(getTargetPart(), resources[0]);
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(getShell(), null, null, e, CVSUIPlugin.LOG_NONTEAM_EXCEPTIONS);
		} catch (InterruptedException e) {
			// Ignore
		}
	}

	 private IResource[] getDeepResourcesToPatch(IProgressMonitor monitor) throws CoreException {
	        ResourceMapping[] mappings = getCVSResourceMappings();
	        List roots = new ArrayList();
	        for (int i = 0; i < mappings.length; i++) {
	            ResourceMapping mapping = mappings[i];
	            ResourceTraversal[] traversals = mapping.getTraversals(
	            		SubscriberResourceMappingContext.createContext(CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber()), 
	            		monitor);
	            for (int j = 0; j < traversals.length; j++) {
	                ResourceTraversal traversal = traversals[j];
	                IResource[] resources = traversal.getResources();
	                if (traversal.getDepth() == IResource.DEPTH_INFINITE) {
	                    roots.addAll(Arrays.asList(resources));
	                } else if (traversal.getDepth() == IResource.DEPTH_ZERO) {
	                    collectShallowFiles(resources, roots);
	                } else if (traversal.getDepth() == IResource.DEPTH_ONE) {
	                    collectShallowFiles(resources, roots);
	                    for (int k = 0; k < resources.length; k++) {
	                        IResource resource = resources[k];
	                        if (resource.getType() != IResource.FILE) {
	                            collectShallowFiles(members(resource), roots);
	                        }
	                    }
	                }
	            }
	        }
	        return (IResource[]) roots.toArray(new IResource[roots.size()]);
	    }
	 
	   private void collectShallowFiles(IResource[] resources, List roots) {
	        for (int k = 0; k < resources.length; k++) {
	            IResource resource = resources[k];
	            if (resource.getType() == IResource.FILE)
	                roots.add(resource);
	        }
	    }
	   
	   private IResource[] members(IResource resource) throws CoreException {
	        return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().members(resource);
	    }
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForMultipleResources()
	 */
	@Override
	protected boolean isEnabledForMultipleResources() {
			return true;
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForUnmanagedResources()
	 */
	@Override
	protected boolean isEnabledForUnmanagedResources() {
		return true;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getId()
	 */
	@Override
	public String getId() {
		return ICVSUIConstants.CMD_CREATEPATCH;
	}
}
