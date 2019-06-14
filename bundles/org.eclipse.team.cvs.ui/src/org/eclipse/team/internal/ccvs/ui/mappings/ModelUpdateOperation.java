/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.mappings;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.client.PruneFolderVisitor;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.core.mapping.CompoundResourceTraversal;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IWorkbenchPart;

public class ModelUpdateOperation extends AbstractModelMergeOperation {
	
	public ModelUpdateOperation(IWorkbenchPart targetPart, ResourceMapping[] selectedResourceMappings, boolean consultModels) {
		this(targetPart, WorkspaceSubscriberContext.createUpdateScopeManager(selectedResourceMappings, consultModels));
	}
	
	public ModelUpdateOperation(IWorkbenchPart targetPart, ResourceMapping[] resourceMappings) {
		this(targetPart, resourceMappings, true);
	}

	public ModelUpdateOperation(IWorkbenchPart targetPart, SubscriberScopeManager manager) {
		super(targetPart, manager, true);
	}

	@Override
	protected String getJobName() {
		return CVSUIMessages.UpdateOperation_taskName;
	}
	
	@Override
	public boolean isPreviewRequested() {
		return super.isPreviewRequested() || !isAttemptHeadlessMerge();
	}

	protected boolean isAttemptHeadlessMerge() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getString(ICVSUIConstants.PREF_UPDATE_HANDLING).equals(ICVSUIConstants.PREF_UPDATE_HANDLING_PERFORM);
	}

	/**
	 * Return the merge type associated with this operation.
	 * @return the merge type associated with this operation
	 */
	protected int getMergeType() {
		return ISynchronizationContext.THREE_WAY;
	}
	
	@Override
	protected ModelSynchronizeParticipant createParticipant() {
		return new WorkspaceModelParticipant(createMergeContext());
	}

	@Override
	protected SynchronizationContext createMergeContext() {
		return WorkspaceSubscriberContext.createContext(getScopeManager(), getMergeType());
	}
	
	@Override
	protected void executeMerge(IProgressMonitor monitor) throws CoreException {
		super.executeMerge(monitor);
		// Prune any empty folders within the traversals
		if (CVSProviderPlugin.getPlugin().getPruneEmptyDirectories()) {
			CompoundResourceTraversal ct = new CompoundResourceTraversal();
			ct.addTraversals(getContext().getScope().getTraversals());
			IResource[] roots = ct.getRoots();
			List<ICVSResource> cvsResources = new ArrayList<>();
			for (IResource resource : roots) {
				if (resource.getProject().isAccessible()) {
					cvsResources.add(CVSWorkspaceRoot.getCVSResourceFor(resource));
				}
			}
			new PruneFolderVisitor().visit(
				CVSWorkspaceRoot.getCVSFolderFor(ResourcesPlugin.getWorkspace().getRoot()),
				cvsResources.toArray(new ICVSResource[cvsResources.size()]));
		}
	}
}
