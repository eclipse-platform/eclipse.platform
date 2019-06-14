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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.core.synchronize.SyncInfoFilter.ContentComparisonSyncInfoFilter;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.*;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Resets the dirty state of files whose contents match their base.
 */
public class RefreshDirtyStateOperation extends CVSSubscriberOperation {
	
	protected RefreshDirtyStateOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		super(configuration, elements);
	}

	@Override
	protected void runWithProjectRule(IProject project, SyncInfoSet set, IProgressMonitor monitor) throws TeamException {
		final SyncInfo[] infos = set.getSyncInfos();
		if (infos.length == 0) return;
		monitor.beginTask(null, 200);
		ensureBaseContentsCached(project, infos, Policy.subMonitorFor(monitor, 100));
		performCleanTimestamps(project, infos, monitor);
		monitor.done();
	}

	private void performCleanTimestamps(IProject project, final SyncInfo[] infos, IProgressMonitor monitor) throws CVSException {
		ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(project);
		final ContentComparisonSyncInfoFilter comparator = new SyncInfoFilter.ContentComparisonSyncInfoFilter(false);
		folder.run(monitor1 -> {
			monitor1.beginTask(null, infos.length * 100);
			for (SyncInfo info : infos) {
				IResource resource = info.getLocal();
				if (resource.getType() == IResource.FILE) {
					if (comparator.compareContents((IFile) resource, info.getBase(),
							Policy.subMonitorFor(monitor1, 100))) {
						ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor((IFile) resource);
						cvsFile.checkedIn(null, false /* not a commit */);
					}
				}
			}
			monitor1.done();
		}, Policy.subMonitorFor(monitor, 100));
	}
	
	private void ensureBaseContentsCached(IProject project, SyncInfo[] infos, IProgressMonitor monitor) throws CVSException {
		List<IDiff> diffs = new ArrayList<>();
		for (SyncInfo info : infos) {
			IDiff node = getConverter().getDeltaFor(info);
			diffs.add(node);
		}
		ensureBaseContentsCached(project, diffs.toArray(new IDiff[diffs.size()]), monitor);
	}

	private SyncInfoToDiffConverter getConverter() {
		SyncInfoToDiffConverter converter = CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().getAdapter(SyncInfoToDiffConverter.class);
		if (converter == null)
			return SyncInfoToDiffConverter.getDefault();
		return converter;
	}

	private void ensureBaseContentsCached(final IProject project, IDiff[] nodes, IProgressMonitor monitor) throws CVSException {
		try {
			ResourceDiffTree tree = new ResourceDiffTree();
			for (IDiff node : nodes) {
				tree.add(node);
			}
			new CacheBaseContentsOperation(getPart(), new ResourceMapping[] { project.getAdapter(ResourceMapping.class) },
					tree, true) {
				@Override
				protected ResourceMappingContext getResourceMappingContext() {
					return new SingleProjectSubscriberContext(CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber(), false, project);
				}
				@Override
				protected SynchronizationScopeManager createScopeManager(boolean consultModels) {
					return new SingleProjectScopeManager(getJobName(), getSelectedMappings(), getResourceMappingContext(), consultModels, project);
				}
			}.run(monitor);
		} catch (InvocationTargetException e) {
			throw CVSException.wrapException(e);
		} catch (InterruptedException e) {
			throw new OperationCanceledException();
		}
	}
	
	@Override
	protected String getErrorTitle() {
		return CVSUIMessages.RefreshDirtyStateOperation_0; 
	}
	
	@Override
	protected String getJobName() {
		return CVSUIMessages.RefreshDirtyStateOperation_1; 
	}
}
