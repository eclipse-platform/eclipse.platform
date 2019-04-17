/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.ICache;
import org.eclipse.team.core.ICacheListener;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * A commit action that will commit all outgoing changes in the context.
 */
public class WorkspaceCommitAction extends AbstractCommitAction implements IDiffChangeListener {

	/**
	 * Create the action
	 * @param configuration the synchronize page configuration
	 */
	public WorkspaceCommitAction(ISynchronizePageConfiguration configuration) {
		super(configuration);
		setId(ICVSUIConstants.CMD_COMMIT_ALL);
		setActionDefinitionId(ICVSUIConstants.CMD_COMMIT_ALL);
		final IDiffTree tree = getDiffTree();
		tree.addDiffChangeListener(this);
		getSynchronizationContext().getCache().addCacheListener(new ICacheListener() {
			@Override
			public void cacheDisposed(ICache cache) {
				tree.removeDiffChangeListener(WorkspaceCommitAction.this);
			}
		});
		updateEnablement();
		
	}
	
	@Override
	protected String getBundleKeyPrefix() {
		return "WorkspaceToolbarCommitAction."; //$NON-NLS-1$
	}

	private IDiffTree getDiffTree() {
		ISynchronizationContext context = (ISynchronizationContext)getConfiguration().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT);
		IDiffTree tree = context.getDiffTree();
		return tree;
	}

	@Override
	protected boolean isEnabledForSelection(IStructuredSelection selection) {
		// Enablement has nothing to do with selection
		return isEnabled();
	}

	@Override
	public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
		updateEnablement();
	}

	@Override
	public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
		// Do nothing
	}
	
	@Override
	public void updateEnablement() {
		boolean enabled = (getDiffTree().countFor(IThreeWayDiff.OUTGOING, IThreeWayDiff.DIRECTION_MASK) > 0)
			&& (getDiffTree().countFor(IThreeWayDiff.CONFLICTING, IThreeWayDiff.DIRECTION_MASK) == 0);
		setEnabled(enabled);
	}
	
	@Override
	protected IResource[] getTargetResources() {
		return getSynchronizationContext().getScope().getRoots();
	}

	@Override
	protected ResourceTraversal[] getCommitTraversals(IStructuredSelection selection, IProgressMonitor monitor)
			throws CoreException {
		return getSynchronizationContext().getScope().getTraversals();
	}

}
