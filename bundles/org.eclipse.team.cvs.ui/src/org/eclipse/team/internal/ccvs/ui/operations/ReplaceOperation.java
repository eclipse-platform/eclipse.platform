/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 *     Matt McCutchen <hashproduct+eclipse@gmail.com> - Bug 179174 CVS client sets timestamps back when replacing
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.PrepareForReplaceVisitor;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.tags.TagSource;
import org.eclipse.team.internal.ccvs.ui.tags.TagSourceWorkbenchAdapter;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This operation replaces the local resources with their remote contents
 */
public class ReplaceOperation extends UpdateOperation {

	public ReplaceOperation(IWorkbenchPart part, IResource[] resources, CVSTag tag, boolean recurse) {
		super(part, asResourceMappers(resources, recurse ? IResource.DEPTH_INFINITE : IResource.DEPTH_ONE), new LocalOption[] { Update.IGNORE_LOCAL_CHANGES }, tag);
	}

	public ReplaceOperation(IWorkbenchPart part, ResourceMapping[] mappings, CVSTag tag) {
		super(part, mappings, new LocalOption[] { Update.IGNORE_LOCAL_CHANGES }, tag);
	}

	@Override
	protected String getTaskName() {
		return CVSUIMessages.ReplaceOperation_taskName; 
	}

	@Override
	protected IStatus executeCommand(
		final Session session,
		final CVSTeamProvider provider,
		final ICVSResource[] resources,
		final boolean recurse, IProgressMonitor monitor)
		throws CVSException, InterruptedException {
		
		final IStatus[] status = new IStatus[] { Status.OK_STATUS };
		try {
			ResourcesPlugin.getWorkspace().run((IWorkspaceRunnable) monitor1 -> {
				try {
					status[0] = internalExecuteCommand(session, provider, resources, recurse, monitor1);
				} catch (InterruptedException e) {
					throw new OperationCanceledException();
				}
			}, null, IWorkspace.AVOID_UPDATE, monitor);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
		return status[0];
	}

	// Files deleted by the PrepareForReplaceVisitor.
	private Set/*<ICVSFile>*/ prepDeletedFiles;

	/**
	 * Tells whether resources for which the given tag does not exists, are ignored by the replace
	 * operation.
	 */
	private boolean ignoreResourcesIfTagDoesNotExist;


	private IStatus internalExecuteCommand(Session session, CVSTeamProvider provider, ICVSResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException {
		monitor.beginTask(null, 100);
		ICVSResource[] managedResources = getResourcesToUpdate(resources, Policy.subMonitorFor(monitor, 5));
		if (ignoreResourcesIfTagDoesNotExist && managedResources.length == 0)
			return OK;

		try {
			// Purge any unmanaged or added files
			PrepareForReplaceVisitor pfrv = new PrepareForReplaceVisitor(session, getTag());
			pfrv.visitResources(
				provider.getProject(), 
				resources, 
				CVSUIMessages.ReplaceOperation_1, 
				recurse ? IResource.DEPTH_INFINITE : IResource.DEPTH_ONE, 
				Policy.subMonitorFor(monitor, 25));
			prepDeletedFiles = pfrv.getDeletedFiles();
			
			// Only perform the remote command if some of the resources being replaced were managed
			IStatus status = OK;
			if (managedResources.length > 0) {
				// Perform an update, ignoring any local file modifications
				status = super.executeCommand(session, provider, managedResources, recurse, Policy.subMonitorFor(monitor, 70));
			}
			
			// Prune any empty folders left after the resources were purged.
			// This is done to prune any empty folders that contained only unmanaged resources
			if (status.isOK() && CVSProviderPlugin.getPlugin().getPruneEmptyDirectories()) {
				new PruneFolderVisitor().visit(session, resources);
			}
			
			return status;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Return the resources that need to be updated from the server.
	 * By default, this is all resources that are managed.
	 * @param resources all resources being replaced
	 * @return resources that are to be updated from the server
	 * @throws CVSException
	 */
	protected ICVSResource[] getResourcesToUpdate(ICVSResource[] resources, IProgressMonitor monitor) throws CVSException {
		// Accumulate the managed resources from the list of provided resources
		List<ICVSResource> managedResources = new ArrayList<>();
		monitor.beginTask(null, resources.length * 100);
		for (ICVSResource resource : resources) {
			if ((resource.isFolder() && ((ICVSFolder)resource).isCVSFolder())) {
				addResourceIfTagExists(managedResources, resource, Policy.subMonitorFor(monitor, 100));
			} else if (!resource.isFolder()){
				byte[] syncBytes = ((ICVSFile)resource).getSyncBytes();
				if (syncBytes != null && !ResourceSyncInfo.isAddition(syncBytes)) {
					addResourceIfTagExists(managedResources, resource, Policy.subMonitorFor(monitor, 100));
				}
			}
		}
		monitor.done();
		return managedResources.toArray(new ICVSResource[managedResources.size()]);
	}

	private void addResourceIfTagExists(List<ICVSResource> managedResources, ICVSResource resource,
			IProgressMonitor monitor) {
		CVSTag tag = getTag();
		if (tag == null || tag.getType() == CVSTag.DATE || tag.isHeadTag() || tag.isBaseTag()) {
			// No need to check for date, head or base tags
			managedResources.add(resource);
		} else {
			TagSource tagSource= TagSource.create(new ICVSResource[] { resource });
			if (isTagPresent(tagSource, tag)) {
				managedResources.add(resource);
			} else {
				// If the tag usn't present, lets refresh just to make sure
				try {
					tagSource.refresh(false, monitor);
					if (isTagPresent(tagSource, tag)) {
						managedResources.add(resource);
					} else {
						tagSource.refresh(true, monitor);
						if (isTagPresent(tagSource, tag)) {
							managedResources.add(resource);
						}
					}
				} catch (TeamException e) {
					CVSUIPlugin.log(e);
				}
			}
		}
	}
	
	private boolean isTagPresent(TagSource tagSource, CVSTag tag) {
		CVSTag[] tags= tagSource.getTags(TagSource.convertIncludeFlaqsToTagTypes(TagSourceWorkbenchAdapter.INCLUDE_ALL_TAGS));
		return Arrays.asList(tags).contains(tag);
	}
	
	@Override
	protected Update getUpdateCommand() {
		// Use a special replace command that doesn't set back the timestamps
		// of files in the passed set if it recreates them.
		return new Replace(prepDeletedFiles);
	}

	@Override
	protected String getTaskName(CVSTeamProvider provider) {
		return NLS.bind(CVSUIMessages.ReplaceOperation_0, new String[] { provider.getProject().getName() }); 
	}

	/**
	 * Tells to ignore resources for which the given tag does not exists.
	 */
	public void ignoreResourcesWithMissingTag() {
		ignoreResourcesIfTagDoesNotExist= true;
	}
}
