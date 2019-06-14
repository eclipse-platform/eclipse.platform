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
package org.eclipse.team.internal.ccvs.ui.operations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.listeners.IUpdateMessageListener;
import org.eclipse.team.internal.ccvs.core.client.listeners.UpdateListener;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Abstract operation for caching the contents for any files
 * in a particular remote tree that differ from the local contents.*
 */
public abstract class CacheTreeContentsOperation extends SingleCommandOperation {

	private final IResourceDiffTree tree;

	public CacheTreeContentsOperation(IWorkbenchPart part, ResourceMapping[] mappings, IResourceDiffTree tree) {
		super(part, mappings, Command.NO_LOCAL_OPTIONS);
		this.tree = tree;
	}
	
	@Override
	protected void execute(CVSTeamProvider provider, IResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException {
		IResource[] files = getFilesWithUncachedContents(resources, recurse);
		if (files.length > 0)
			super.execute(provider, files, recurse, monitor);
	}
	
	private IResource[] getFilesWithUncachedContents(IResource[] resources, boolean recurse) {
		ArrayList<IResource> result = new ArrayList<>();
		for (IResource resource : resources) {
			IDiff[] nodes = tree.getDiffs(resource, recurse ? IResource.DEPTH_INFINITE: IResource.DEPTH_ONE);
			for (IDiff node : nodes) {
				if (needsContents(node)) {
					result.add(tree.getResource(node));
				}
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	protected boolean needsContents(IDiff node) {
		if (node instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) node;	
			IResource local = getTree().getResource(node);
			IFileRevision remote = getRemoteFileState(twd);
			if (remote != null) {
				IResourceVariant variant = Adapters.adapt(remote, IResourceVariant.class);
				if (local.getType() == IResource.FILE 
						&& isEnabledForDirection(twd.getDirection()) 
						&& variant instanceof RemoteFile) {
					RemoteFile rf = (RemoteFile) variant;
					if (!rf.isContentsCached()) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Get the remote file state that is of interest.
	 * @param twd a three way diff
	 * @return the remote file state that is of interest
	 */
	protected abstract IFileRevision getRemoteFileState(IThreeWayDiff twd);

	/**
	 * Return whether the direction is of interest.
	 * @param direction the direction of a diff
	 * @return whether the direction is of interest
	 */
	protected abstract boolean isEnabledForDirection(int direction);

	/*
	 * Use a local root that is really the base tree so we can cache the base
	 * contents without affecting the local contents.
	 * 
	 * @see
	 * org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#
	 * getLocalRoot(org.eclipse.team.internal.ccvs.core.CVSTeamProvider)
	 */
	@Override
	protected ICVSFolder getLocalRoot(CVSTeamProvider provider)
			throws CVSException {
		try {
			ICVSRemoteResource tree = buildTree(provider);
			return (ICVSFolder)tree;
		} catch (TeamException e) {
			throw CVSException.wrapException(e);
		}
	}

	protected abstract ICVSRemoteResource buildTree(CVSTeamProvider provider) throws TeamException;
	
	@Override
	protected ICVSResource[] getCVSArguments(Session session, IResource[] resources) {
		List<ICVSResource> result = new ArrayList<>();
		for (IResource resource : resources) {
			try {
				ICVSResource file = session.getLocalRoot().getChild(resource.getProjectRelativePath().toString());
				result.add(file);
			} catch (CVSException e) {
				// Log and continue
				CVSUIPlugin.log(e);
			}
		}

		return result.toArray(new ICVSResource[result.size()]);
	}
	
	@Override
	protected IStatus executeCommand(Session session, CVSTeamProvider provider, ICVSResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException {
		return Command.UPDATE.execute(
				session,
				Command.NO_GLOBAL_OPTIONS,
				getLocalOptions(true),
				resources,
				new UpdateListener(new IUpdateMessageListener() {
					@Override
					public void fileInformation(int type, ICVSFolder parent, String filename) {
						// Do nothing
					}
					@Override
					public void fileDoesNotExist(ICVSFolder parent, String filename) {
						// Do nothing
					}
					@Override
					public void directoryInformation(ICVSFolder commandRoot, String path,
							boolean newDirectory) {
						// Do nothing
					}
					@Override
					public void directoryDoesNotExist(ICVSFolder commandRoot, String path) {
						// Do nothing
					}
				}),
				monitor);
	}

	@Override
	protected LocalOption[] getLocalOptions(boolean recurse) {
		return Update.IGNORE_LOCAL_CHANGES.addTo(super.getLocalOptions(recurse));
	}
	
	@Override
	protected String getTaskName(CVSTeamProvider provider) {
		return NLS.bind(CVSUIMessages.CacheTreeContentsOperation_0, new String[] {provider.getProject().getName()});
	}

	@Override
	protected String getTaskName() {
		return CVSUIMessages.CacheTreeContentsOperation_1;
	}

	/**
	 * Return the diff tree whose contents are being cached
	 * @return
	 */
	protected IResourceDiffTree getTree() {
		return tree;
	}
	
	@Override
	public boolean consultModelsForMappings() {
		return false;
	}
	
	@Override
	protected boolean isReportableError(IStatus status) {
		return super.isReportableError(status) && status.getSeverity() == IStatus.ERROR;
	}

}
