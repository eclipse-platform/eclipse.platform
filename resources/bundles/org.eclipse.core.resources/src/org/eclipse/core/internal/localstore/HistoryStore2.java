/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.localstore;

import java.io.InputStream;
import java.util.*;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.internal.localstore.Bucket.Entry;
import org.eclipse.core.internal.localstore.HistoryBucket.HistoryEntry;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

public class HistoryStore2 implements IHistoryStore {

	class HistoryCopyVisitor extends Bucket.Visitor {
		private List<HistoryEntry> changes = new ArrayList<>();
		private IPath destination;
		private IPath source;

		public HistoryCopyVisitor(IPath source, IPath destination) {
			this.source = source;
			this.destination = destination;
		}

		@Override
		public void afterSaving(Bucket bucket) throws CoreException {
			saveChanges();
			changes.clear();
		}

		private void saveChanges() throws CoreException {
			if (changes.isEmpty())
				return;
			// make effective all changes collected
			Iterator<HistoryEntry> i = changes.iterator();
			HistoryEntry entry = i.next();
			tree.loadBucketFor(entry.getPath());
			HistoryBucket bucket = (HistoryBucket) tree.getCurrent();
			bucket.addBlobs(entry);
			while (i.hasNext())
				bucket.addBlobs(i.next());
			bucket.save();
		}

		@Override
		public int visit(Entry sourceEntry) {
			IPath destinationPath = destination.append(sourceEntry.getPath().removeFirstSegments(source.segmentCount()));
			HistoryEntry destinationEntry = new HistoryEntry(destinationPath, (HistoryEntry) sourceEntry);
			// we may be copying to the same source bucket, collect to make change effective later
			// since we cannot make changes to it while iterating
			changes.add(destinationEntry);
			return CONTINUE;
		}
	}

	private BlobStore blobStore;
	private Set<UniversalUniqueIdentifier> blobsToRemove = new HashSet<>();
	final BucketTree tree;
	private Workspace workspace;

	public HistoryStore2(Workspace workspace, IFileStore store, int limit) {
		this.workspace = workspace;
		try {
			store.mkdir(EFS.NONE, null);
		} catch (CoreException e) {
			//ignore the failure here because there is no way to surface it.
			//any attempt to write to the store will throw an appropriate exception
		}
		this.blobStore = new BlobStore(store, limit);
		this.tree = new BucketTree(workspace, new HistoryBucket());
	}

	/**
	 * @see IHistoryStore#addState(IPath, IFileStore, IFileInfo, boolean)
	 */
	@Override
	public synchronized IFileState addState(IPath key, IFileStore localFile, IFileInfo info, boolean moveContents) {
		long lastModified = info.getLastModified();
		if (Policy.DEBUG_HISTORY)
			Policy.debug("History: Adding state for key: " + key + ", file: " + localFile + ", timestamp: " + lastModified + ", size: " + localFile.fetchInfo().getLength()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		if (!isValid(localFile, info))
			return null;
		UniversalUniqueIdentifier uuid = null;
		try {
			uuid = blobStore.addBlob(localFile, moveContents);
			tree.loadBucketFor(key);
			HistoryBucket currentBucket = (HistoryBucket) tree.getCurrent();
			currentBucket.addBlob(key, uuid, lastModified);
			//			currentBucket.save();
		} catch (CoreException e) {
			log(e);
		}
		return new FileState(this, key, lastModified, uuid);
	}

	@Override
	public synchronized Set<IPath> allFiles(IPath root, int depth, IProgressMonitor monitor) {
		final Set<IPath> allFiles = new HashSet<>();
		try {
			tree.accept(new Bucket.Visitor() {
				@Override
				public int visit(Entry fileEntry) {
					allFiles.add(fileEntry.getPath());
					return CONTINUE;
				}
			}, root, depth == IResource.DEPTH_INFINITE ? BucketTree.DEPTH_INFINITE : depth);
		} catch (CoreException e) {
			log(e);
		}
		return allFiles;
	}

	/**
	 * Applies the clean-up policy to an entry.
	 */
	protected void applyPolicy(HistoryEntry fileEntry, int maxStates, long minTimeStamp) {
		for (int i = 0; i < fileEntry.getOccurrences(); i++) {
			if (i < maxStates && fileEntry.getTimestamp(i) >= minTimeStamp)
				continue;
			// "delete" the current uuid
			blobsToRemove.add(fileEntry.getUUID(i));
			fileEntry.deleteOccurrence(i);
		}
	}

	/**
	 * Applies the clean-up policy to a subtree.
	 */
	private void applyPolicy(IPath root) throws CoreException {
		IWorkspaceDescription description = workspace.internalGetDescription();
		final long minimumTimestamp = System.currentTimeMillis() - description.getFileStateLongevity();
		final int maxStates = description.getMaxFileStates();
		// apply policy to the given tree
		tree.accept(new Bucket.Visitor() {
			@Override
			public int visit(Entry entry) {
				applyPolicy((HistoryEntry) entry, maxStates, minimumTimestamp);
				return CONTINUE;
			}
		}, root, BucketTree.DEPTH_INFINITE);
		tree.getCurrent().save();
	}

	@Override
	public synchronized void clean(final IProgressMonitor monitor) {
		long start = System.currentTimeMillis();
		try {
			monitor.beginTask(Messages.resources_pruningHistory, IProgressMonitor.UNKNOWN);
			IWorkspaceDescription description = workspace.internalGetDescription();
			final long minimumTimestamp = System.currentTimeMillis() - description.getFileStateLongevity();
			final int maxStates = description.getMaxFileStates();
			final int[] entryCount = new int[1];
			if (description.isApplyFileStatePolicy()) {
				tree.accept(new Bucket.Visitor() {
					@Override
					public int visit(Entry fileEntry) {
						if (monitor.isCanceled())
							return STOP;
						entryCount[0] += fileEntry.getOccurrences();
						applyPolicy((HistoryEntry) fileEntry, maxStates, minimumTimestamp);
						// remove unreferenced blobs, when blobsToRemove size is greater than 100
						removeUnreferencedBlobs(100);
						return monitor.isCanceled() ? STOP : CONTINUE;
					}
				}, Path.ROOT, BucketTree.DEPTH_INFINITE);
			}
			if (Policy.DEBUG_HISTORY) {
				Policy.debug("Time to apply history store policies: " + (System.currentTimeMillis() - start) + "ms."); //$NON-NLS-1$ //$NON-NLS-2$
				Policy.debug("Total number of history store entries: " + entryCount[0]); //$NON-NLS-1$
			}
			// remove all remaining unreferenced blobs
			removeUnreferencedBlobs(0);
		} catch (Exception e) {
			String message = Messages.history_problemsCleaning;
			ResourceStatus status = new ResourceStatus(IResourceStatus.FAILED_DELETE_LOCAL, null, message, e);
			Policy.log(status);
		} finally {
			monitor.done();
		}
	}

	/*
	 * Remove blobs from the blobStore. When the size of blobsToRemove exceeds the limit,
	 * remove the given blobs from blobStore. If the limit is zero or negative, remove blobs
	 * regardless of the limit.
	 */
	void removeUnreferencedBlobs(int limit) {
		if (limit <= 0 || limit <= blobsToRemove.size()) {
			long start = System.currentTimeMillis();
			// remove unreferenced blobs
			blobStore.deleteBlobs(blobsToRemove);
			if (Policy.DEBUG_HISTORY)
				Policy.debug("Time to remove " + blobsToRemove.size() + " unreferenced blobs: " + (System.currentTimeMillis() - start) + "ms."); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			blobsToRemove = new HashSet<>();
		}
	}

	@Override
	public void closeHistoryStore(IResource resource) {
		try {
			tree.getCurrent().save();
			tree.getCurrent().flush();
		} catch (CoreException e) {
			log(e);
		}
	}

	@Override
	public synchronized void copyHistory(IResource sourceResource, IResource destinationResource, boolean moving) {
		// return early if either of the paths are null or if the source and
		// destination are the same.
		if (sourceResource == null || destinationResource == null) {
			String message = Messages.history_copyToNull;
			ResourceStatus status = new ResourceStatus(IResourceStatus.INTERNAL_ERROR, null, message, null);
			Policy.log(status);
			return;
		}
		if (sourceResource.equals(destinationResource)) {
			String message = Messages.history_copyToSelf;
			ResourceStatus status = new ResourceStatus(IResourceStatus.INTERNAL_ERROR, sourceResource.getFullPath(), message, null);
			Policy.log(status);
			return;
		}

		final IPath source = sourceResource.getFullPath();
		final IPath destination = destinationResource.getFullPath();
		Assert.isLegal(source.segmentCount() > 0);
		Assert.isLegal(destination.segmentCount() > 0);
		Assert.isLegal(source.segmentCount() > 1 || destination.segmentCount() == 1);

		try {
			// special case: we are moving a project
			if (moving && sourceResource.getType() == IResource.PROJECT) {
				// flush the tree to avoid confusion if another project is created with the same name
				final Bucket bucket = tree.getCurrent();
				bucket.save();
				bucket.flush();
				return;
			}
			// copy history by visiting the source tree
			HistoryCopyVisitor copyVisitor = new HistoryCopyVisitor(source, destination);
			tree.accept(copyVisitor, source, BucketTree.DEPTH_INFINITE);
			// apply clean-up policy to the destination tree
			applyPolicy(destinationResource.getFullPath());
		} catch (CoreException e) {
			log(e);
		}
	}

	@Override
	public boolean exists(IFileState target) {
		return blobStore.fileFor(((FileState) target).getUUID()).fetchInfo().exists();
	}

	@Override
	public InputStream getContents(IFileState target) throws CoreException {
		if (!target.exists()) {
			String message = Messages.history_notValid;
			throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL, target.getFullPath(), message, null);
		}
		return blobStore.getBlob(((FileState) target).getUUID());
	}

	@Override
	public synchronized IFileState[] getStates(IPath filePath, IProgressMonitor monitor) {
		try {
			tree.loadBucketFor(filePath);
			HistoryBucket currentBucket = (HistoryBucket) tree.getCurrent();
			HistoryEntry fileEntry = currentBucket.getEntry(filePath);
			if (fileEntry == null || fileEntry.isEmpty())
				return new IFileState[0];
			IFileState[] states = new IFileState[fileEntry.getOccurrences()];
			for (int i = 0; i < states.length; i++)
				states[i] = new FileState(this, fileEntry.getPath(), fileEntry.getTimestamp(i), fileEntry.getUUID(i));
			return states;
		} catch (CoreException ce) {
			log(ce);
			return new IFileState[0];
		}
	}

	public BucketTree getTree() {
		return tree;
	}

	/**
	 * Return a boolean value indicating whether or not the given file
	 * should be added to the history store based on the current history
	 * store policies.
	 *
	 * @param localFile the file to check
	 * @return <code>true</code> if this file should be added to the history
	 * 	store and <code>false</code> otherwise
	 */
	private boolean isValid(IFileStore localFile, IFileInfo info) {
		WorkspaceDescription description = workspace.internalGetDescription();
		if (!description.isApplyFileStatePolicy())
			return true;
		long length = info.getLength();
		boolean result = length <= description.getMaxFileStateSize();
		if (Policy.DEBUG_HISTORY && !result)
			Policy.debug("History: Ignoring file (too large). File: " + localFile.toString() + //$NON-NLS-1$
					", size: " + length + //$NON-NLS-1$
					", max: " + description.getMaxFileStateSize()); //$NON-NLS-1$
		return result;
	}

	/**
	 * Logs a CoreException
	 */
	private void log(CoreException e) {
		//create a new status to wrap the exception if there is no exception in the status
		IStatus status = e.getStatus();
		if (status.getException() == null)
			status = new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_WRITE_METADATA, "Internal error in history store", e); //$NON-NLS-1$
		Policy.log(status);
	}

	@Override
	public synchronized void remove(IPath root, IProgressMonitor monitor) {
		try {
			final Set<UniversalUniqueIdentifier> tmpBlobsToRemove = blobsToRemove;
			tree.accept(new Bucket.Visitor() {
				@Override
				public int visit(Entry fileEntry) {
					for (int i = 0; i < fileEntry.getOccurrences(); i++)
						// remember we need to delete the files later
						tmpBlobsToRemove.add(((HistoryEntry) fileEntry).getUUID(i));
					fileEntry.delete();
					return CONTINUE;
				}
			}, root, BucketTree.DEPTH_INFINITE);
		} catch (CoreException ce) {
			log(ce);
		}
	}

	/**
	 * @see IHistoryStore#removeGarbage()
	 */
	@Override
	public synchronized void removeGarbage() {
		try {
			final Set<UniversalUniqueIdentifier> tmpBlobsToRemove = blobsToRemove;
			tree.accept(new Bucket.Visitor() {
				@Override
				public int visit(Entry fileEntry) {
					for (int i = 0; i < fileEntry.getOccurrences(); i++)
						// remember we need to delete the files later
						tmpBlobsToRemove.remove(((HistoryEntry) fileEntry).getUUID(i));
					return CONTINUE;
				}
			}, Path.ROOT, BucketTree.DEPTH_INFINITE);
			blobStore.deleteBlobs(blobsToRemove);
			blobsToRemove = new HashSet<>();
		} catch (Exception e) {
			String message = Messages.history_problemsCleaning;
			ResourceStatus status = new ResourceStatus(IResourceStatus.FAILED_DELETE_LOCAL, null, message, e);
			Policy.log(status);
		}
	}

	@Override
	public synchronized void shutdown(IProgressMonitor monitor) throws CoreException {
		tree.close();
	}

	@Override
	public void startup(IProgressMonitor monitor) {
		// nothing to be done
	}
}
