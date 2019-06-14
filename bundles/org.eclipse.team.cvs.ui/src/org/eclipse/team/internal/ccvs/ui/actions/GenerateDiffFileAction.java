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
		List<IResource> roots = new ArrayList<>();
		for (ResourceMapping mapping : mappings) {
			ResourceTraversal[] traversals = mapping.getTraversals(
					SubscriberResourceMappingContext.createContext(CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber()), 
					monitor);
			for (ResourceTraversal traversal : traversals) {
				IResource[] resources = traversal.getResources();
				if (traversal.getDepth() == IResource.DEPTH_INFINITE) {
					roots.addAll(Arrays.asList(resources));
				} else if (traversal.getDepth() == IResource.DEPTH_ZERO) {
					collectShallowFiles(resources, roots);
				} else if (traversal.getDepth() == IResource.DEPTH_ONE) {
					collectShallowFiles(resources, roots);
					for (IResource resource : resources) {
						if (resource.getType() != IResource.FILE) {
							collectShallowFiles(members(resource), roots);
						}
					}
				}
			}
		}
		return roots.toArray(new IResource[roots.size()]);
	}
	
	private void collectShallowFiles(IResource[] resources, List<IResource> roots) {
		for (IResource resource : resources) {
			if (resource.getType() == IResource.FILE)
				roots.add(resource);
		}
	}
	
	private IResource[] members(IResource resource) throws CoreException {
		return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().members(resource);
	}
	@Override
	protected boolean isEnabledForMultipleResources() {
			return true;
	}

	@Override
	protected boolean isEnabledForUnmanagedResources() {
		return true;
	}

	
	@Override
	public String getId() {
		return ICVSUIConstants.CMD_CREATEPATCH;
	}
}
