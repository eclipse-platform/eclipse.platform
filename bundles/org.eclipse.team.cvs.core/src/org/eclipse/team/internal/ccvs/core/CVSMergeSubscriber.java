/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoFilter;
import org.eclipse.team.core.variants.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.syncinfo.CVSResourceVariantTree;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * A CVSMergeSubscriber is responsible for maintaining the remote trees for a merge into
 * the workspace. The remote trees represent the CVS revisions of the start and end
 * points (version or branch) of the merge.
 * 
 * This subscriber stores the remote handles in the resource tree sync info slot. When
 * the merge is cancelled this sync info is cleared.
 * 
 * A merge can persist between workbench sessions and thus can be used as an
 * ongoing merge.
 * 
 * TODO: Is the merge subscriber interested in workspace sync info changes?
 * TODO: Do certain operations (e.g. replace with) invalidate a merge subscriber?
 * TODO: How to ensure that sync info is flushed when merge roots are deleted?
 */
public class CVSMergeSubscriber extends CVSSyncTreeSubscriber implements IResourceChangeListener, ISubscriberChangeListener {

	private final class MergeBaseTree extends CVSResourceVariantTree {
		// The merge synchronizer has been kept so that those upgrading
		// from 3.0 M8 to 3.0 M9 so not lose there ongoing merge state
		private PersistantResourceVariantByteStore mergedSynchronizer;
		private MergeBaseTree(ResourceVariantByteStore cache, CVSTag tag, boolean cacheFileContentsHint, String syncKeyPrefix) {
			super(cache, tag, cacheFileContentsHint);
			mergedSynchronizer = new PersistantResourceVariantByteStore(new QualifiedName(SYNC_KEY_QUALIFIER, syncKeyPrefix + "0merged")); //$NON-NLS-1$
		}
		public IResource[] refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
			// Only refresh the base of a resource once as it should not change
			List unrefreshed = new ArrayList();
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				if (!hasResourceVariant(resource)) {
					unrefreshed.add(resource);
				}
			}
			if (unrefreshed.isEmpty()) {
				monitor.done();
				return new IResource[0];
			}
			IResource[] refreshed = super.refresh((IResource[]) unrefreshed.toArray(new IResource[unrefreshed.size()]), depth, monitor);
			return refreshed;
		}
		public IResourceVariant getResourceVariant(IResource resource) throws TeamException {
			// Use the merged bytes for the base if there are some
			byte[] mergedBytes = mergedSynchronizer.getBytes(resource);
			if (mergedBytes != null) {
				byte[] parentBytes = getByteStore().getBytes(resource.getParent());
				if (parentBytes != null) {
					return RemoteFile.fromBytes(resource, mergedBytes, parentBytes);
				}
			}
			return super.getResourceVariant(resource);
		}
		
		/**
		 * Mark the resource as merged by making it's base equal the remote
		 */
		public void merged(IResource resource, byte[] remoteBytes) throws TeamException {
			if (remoteBytes == null) {
				getByteStore().deleteBytes(resource);
			} else {
				getByteStore().setBytes(resource, remoteBytes);
			}
		}
		
		/**
		 * Return true if the remote has already been merged
		 * (i.e. the base equals the remote).
		 */
		public boolean isMerged(IResource resource, byte[] remoteBytes) throws TeamException {
			byte[] mergedBytes = getByteStore().getBytes(resource);
			return Util.equals(mergedBytes, remoteBytes);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.team.internal.ccvs.core.syncinfo.CVSResourceVariantTree#dispose()
		 */
		public void dispose() {
			mergedSynchronizer.dispose();
			super.dispose();
		}
	}

	public static final String ID = "org.eclipse.team.cvs.ui.cvsmerge-participant"; //$NON-NLS-1$
	public static final String ID_MODAL = "org.eclipse.team.cvs.ui.cvsmerge-participant-modal"; //$NON-NLS-1$
	private static final String UNIQUE_ID_PREFIX = "merge-"; //$NON-NLS-1$
	
	private CVSTag start, end;
	private List roots;
	private CVSResourceVariantTree remoteTree;
	private MergeBaseTree baseTree;
	private boolean isModelSync;

	public CVSMergeSubscriber(IResource[] roots, CVSTag start, CVSTag end, boolean isModelSync) {		
		this(getUniqueId(), roots, start, end);
		this.isModelSync = isModelSync;
	}

	private static QualifiedName getUniqueId() {
		String uniqueId = Long.toString(System.currentTimeMillis());
		return new QualifiedName(ID, "CVS" + UNIQUE_ID_PREFIX + uniqueId); //$NON-NLS-1$
	}
	
	public CVSMergeSubscriber(QualifiedName id, IResource[] roots, CVSTag start, CVSTag end) {		
		super(id, NLS.bind(CVSMessages.CVSMergeSubscriber_2, new String[] { start.getName(), end.getName() }));
		this.start = start;
		this.end = end;
		this.roots = new ArrayList(Arrays.asList(roots));
		initialize();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSWorkspaceSubscriber#initialize()
	 */
	private void initialize() {			
		QualifiedName id = getId();
		String syncKeyPrefix = id.getLocalName();
		PersistantResourceVariantByteStore remoteSynchronizer = new PersistantResourceVariantByteStore(new QualifiedName(SYNC_KEY_QUALIFIER, syncKeyPrefix + end.getName()));
		remoteTree = new CVSResourceVariantTree(remoteSynchronizer, getEndTag(), getCacheFileContentsHint()) {
			public IResource[] refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
				// Override refresh to compare file contents
				monitor.beginTask(null, 100);
				try {
					IResource[] refreshed = super.refresh(resources, depth, monitor);
					compareWithRemote(refreshed, Policy.subMonitorFor(monitor, 50));
					return refreshed;
				} finally {
					monitor.done();
				}
			}
		};
		PersistantResourceVariantByteStore baseSynchronizer = new PersistantResourceVariantByteStore(new QualifiedName(SYNC_KEY_QUALIFIER, syncKeyPrefix + start.getName()));
		baseTree = new MergeBaseTree(baseSynchronizer, getStartTag(), getCacheFileContentsHint(), syncKeyPrefix);
		
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
		CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().addListener(this);
	}

	protected SyncInfo getSyncInfo(IResource local, IResourceVariant base, IResourceVariant remote) throws TeamException {
		CVSMergeSyncInfo info = new CVSMergeSyncInfo(local, base, remote, this);
		info.init();
		return info;
	}

	public void merged(IResource[] resources) throws TeamException {
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			internalMerged(resource);
		}
		fireTeamResourceChange(SubscriberChangeEvent.asSyncChangedDeltas(this, resources));
	}
	
	private void internalMerged(IResource resource) throws TeamException {
		byte[] remoteBytes = getRemoteByteStore().getBytes(resource);
		baseTree.merged(resource, remoteBytes);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.TeamSubscriber#cancel()
	 */
	public void cancel() {	
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);		
		remoteTree.dispose();
		baseTree.dispose();	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.TeamSubscriber#roots()
	 */
	public IResource[] roots() {
		return (IResource[]) roots.toArray(new IResource[roots.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.TeamSubscriber#isSupervised(org.eclipse.core.resources.IResource)
	 */
	public boolean isSupervised(IResource resource) throws TeamException {
		return getBaseTree().hasResourceVariant(resource) || getRemoteTree().hasResourceVariant(resource); 
	}

	public CVSTag getStartTag() {
		return start;
	}
	
	public CVSTag getEndTag() {
		return end;
	}

	boolean isModelSync() {
		return isModelSync;
	}

	/*
	 * What to do when a root resource for this merge changes?
	 * Deleted, Move, Copied
	 * Changed in a CVS way (tag changed, revision changed...)
	 * Contents changed by user
	 * @see IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			IResourceDelta delta = event.getDelta();
			if(delta != null) {
				delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();
			
					if (resource.getType()==IResource.PROJECT) {
						IProject project = (IProject)resource;
						if (!project.isAccessible()) {
							return false;
						}
						if ((delta.getFlags() & IResourceDelta.OPEN) != 0) {
							return false;
						} 
						if (RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId()) == null) {
							return false;
						}
					}
			
					if (roots.contains(resource)) {
						if (delta.getKind() == IResourceDelta.REMOVED || delta.getKind() == IResourceDelta.MOVED_TO) {
							cancel();
						}
						// stop visiting children
						return false;
					}
					// keep visiting children
					return true;
				}
			});
			}
		} catch (CoreException e) {
			CVSProviderPlugin.log(e.getStatus());
		}
	}

	/**
	 * Return whether the given resource has been merged with its 
	 * corresponding remote.
	 * @param resource the local resource
	 * @return boolean
	 * @throws TeamException
	 */
	public boolean isMerged(IResource resource) throws TeamException {
		byte[] remoteBytes = getRemoteByteStore().getBytes(resource);
		return baseTree.isMerged(resource, remoteBytes);
	}

	/* 
	 * Currently only the workspace subscriber knows when a project has been deconfigured. We will listen for these events
	 * and remove the root then forward to merge subscriber listeners.
	 * (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ITeamResourceChangeListener#teamResourceChanged(org.eclipse.team.core.subscribers.TeamDelta[])
	 */
	public void subscriberResourceChanged(ISubscriberChangeEvent[] deltas) {		
		for (int i = 0; i < deltas.length; i++) {
			ISubscriberChangeEvent delta = deltas[i];
			switch(delta.getFlags()) {
				case ISubscriberChangeEvent.ROOT_REMOVED:
					IResource resource = delta.getResource();
					if(roots.remove(resource))	{
						fireTeamResourceChange(new ISubscriberChangeEvent[] {delta});
					}						
					break;
				default:
					break;
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSSyncTreeSubscriber#getBaseSynchronizationCache()
	 */
	protected IResourceVariantTree getBaseTree() {
		return baseTree;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSSyncTreeSubscriber#getRemoteSynchronizationCache()
	 */
	protected IResourceVariantTree getRemoteTree() {
		return remoteTree;
	}
	
	protected  boolean getCacheFileContentsHint() {
		return true;
	}

	/*
	 * Mark as merged any local resources whose contents match that of the remote resource.
	 */
	private void compareWithRemote(IResource[] refreshed, IProgressMonitor monitor) throws CVSException, TeamException {
		// For any remote changes, if the revision differs from the local, compare the contents.
		if (refreshed.length == 0) return;
		SyncInfoFilter.ContentComparisonSyncInfoFilter contentFilter =
			new SyncInfoFilter.ContentComparisonSyncInfoFilter();
		monitor.beginTask(null, refreshed.length * 100);
		for (int i = 0; i < refreshed.length; i++) {
			IResource resource = refreshed[i];
			if (resource.getType() == IResource.FILE) {
				ICVSFile local = CVSWorkspaceRoot.getCVSFileFor((IFile)resource);
				byte[] localBytes = local.getSyncBytes();
				byte[] remoteBytes = getRemoteByteStore().getBytes(resource);
				if (remoteBytes != null 
						&& localBytes != null
						&& local.exists()
						&& !ResourceSyncInfo.getRevision(remoteBytes).equals(ResourceSyncInfo.getRevision(localBytes))
						&& contentFilter.select(getSyncInfo(resource), Policy.subMonitorFor(monitor, 100))) {
					// The contents are equals so mark the file as merged
					internalMerged(resource);
				}
			}
		}
		monitor.done();
	}
	
	
	private PersistantResourceVariantByteStore getRemoteByteStore() {
		return (PersistantResourceVariantByteStore)((CVSResourceVariantTree)getRemoteTree()).getByteStore();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if(this == other) return true;
		if(! (other instanceof CVSMergeSubscriber)) return false;
		CVSMergeSubscriber s = (CVSMergeSubscriber)other;
		return getEndTag().equals(s.getEndTag()) && 
			   getStartTag().equals(s.getStartTag()) && rootsEqual(s);		
	}
}
