/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.core.mapping.provider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.DelegatingStorageMerger;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.core.mapping.IMergeStatus;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.IResourceMappingMerger;
import org.eclipse.team.core.mapping.IStorageMerger;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;

/**
 * Provides the context for an <code>IResourceMappingMerger</code>.
 * It provides access to the ancestor and remote resource mapping contexts
 * so that resource mapping mergers can attempt head-less auto-merges.
 * The ancestor context is only required for merges while the remote
 * is required for both merge and replace.
 *
 * @see IResourceMappingMerger
 * @since 3.2
 */
public abstract class MergeContext extends SynchronizationContext implements IMergeContext {

	/**
	 * Create a merge context.
	 *
	 * @param manager   the manager that defines the scope of the synchronization
	 * @param type      the type of synchronization (ONE_WAY or TWO_WAY)
	 * @param deltaTree the sync info tree that contains all out-of-sync resources
	 */
	protected MergeContext(ISynchronizationScopeManager manager, int type, IResourceDiffTree deltaTree) {
		super(manager, type, deltaTree);
	}

	@Override
	public void reject(final IDiff[] diffs, IProgressMonitor monitor) throws CoreException {
		run(monitor1 -> {
			for (IDiff node : diffs) {
				reject(node, monitor1);
			}
		}, getMergeRule(diffs), IResource.NONE, monitor);
	}

	@Override
	public void markAsMerged(final IDiff[] nodes, final boolean inSyncHint, IProgressMonitor monitor) throws CoreException {
		run(monitor1 -> {
			for (IDiff node : nodes) {
				markAsMerged(node, inSyncHint, monitor1);
			}
		}, getMergeRule(nodes), IResource.NONE, monitor);
	}

	@Override
	public IStatus merge(final IDiff[] deltas, final boolean force, IProgressMonitor monitor) throws CoreException {
		final List<IFile> failedFiles = new ArrayList<>();
		run(monitor1 -> {
			try {
				monitor1.beginTask(null, deltas.length * 100);
				for (IDiff delta : deltas) {
					IStatus s = merge(delta, force, Policy.subMonitorFor(monitor1, 100));
					if (!s.isOK()) {
						if (s.getCode() == IMergeStatus.CONFLICTS) {
							failedFiles.addAll(Arrays.asList(((IMergeStatus)s).getConflictingFiles()));
						} else {
							throw new CoreException(s);
						}
					}
				}
			} finally {
				monitor1.done();
			}
		}, getMergeRule(deltas), IWorkspace.AVOID_UPDATE, monitor);
		if (failedFiles.isEmpty()) {
			return Status.OK_STATUS;
		} else {
			return new MergeStatus(TeamPlugin.ID, Messages.MergeContext_0, failedFiles.toArray(new IFile[failedFiles.size()]));
		}
	}

	@Override
	public IStatus merge(IDiff diff, boolean ignoreLocalChanges, IProgressMonitor monitor) throws CoreException {
		Policy.checkCanceled(monitor);
		IResource resource = getDiffTree().getResource(diff);
		if (resource.getType() != IResource.FILE) {
			if (diff instanceof IThreeWayDiff twd) {
				if ((ignoreLocalChanges || getMergeType() == TWO_WAY)
						&& resource.getType() == IResource.FOLDER
						&& twd.getKind() == IDiff.ADD
						&& twd.getDirection() == IThreeWayDiff.OUTGOING
						&& ((IFolder)resource).members().length == 0) {
					// Delete the local folder addition
					resource.delete(false, monitor);
				} else if (resource.getType() == IResource.FOLDER
						&& !resource.exists()
						&& twd.getKind() == IDiff.ADD
						&& twd.getDirection() == IThreeWayDiff.INCOMING) {
					ensureParentsExist(resource, monitor);
					((IFolder)resource).create(false, true, monitor);
					makeInSync(diff, monitor);
				}
			}
			return Status.OK_STATUS;
		}
		if (diff instanceof IThreeWayDiff twDelta && !ignoreLocalChanges && getMergeType() == THREE_WAY) {
			int direction = twDelta.getDirection();
			if (direction == IThreeWayDiff.OUTGOING) {
				// There's nothing to do so return OK
				return Status.OK_STATUS;
			}
			if (direction == IThreeWayDiff.INCOMING) {
				// Just copy the stream since there are no conflicts
				performReplace(diff, monitor);
				return Status.OK_STATUS;
			}
			// direction == SyncInfo.CONFLICTING
			int type = twDelta.getKind();
			if (type == IDiff.REMOVE) {
				makeInSync(diff, monitor);
				return Status.OK_STATUS;
			}
			// type == SyncInfo.CHANGE
			IResourceDiff remoteChange = (IResourceDiff)twDelta.getRemoteChange();
			IFileRevision remote = null;
			if (remoteChange != null) {
				remote = remoteChange.getAfterState();
			}
			if (remote == null || !getLocalFile(diff).exists()) {
				// Nothing we can do so return a conflict status
				// TODO: Should we handle the case where the local and remote have the same contents for a conflicting addition?
				return new MergeStatus(TeamPlugin.ID, NLS.bind(Messages.MergeContext_1, diff.getPath().toString()),
						new IFile[] { getLocalFile(diff) });
			}
			// We have a conflict, a local, base and remote so we can do
			// a three-way merge
			return performThreeWayMerge(twDelta, monitor);
		} else {
			performReplace(diff, monitor);
			return Status.OK_STATUS;
		}

	}

	/**
	 * Perform a three-way merge on the given three-way diff that contains a content conflict.
	 * By default, this method makes use of {@link IStorageMerger} instances registered
	 * with the <code>storageMergers</code> extension point. Note that the ancestor
	 * of the given diff may be missing. Some {@link IStorageMerger} instances
	 * can still merge without an ancestor so we need to consult the
	 * appropriate merger to find out.
	 * @param diff the diff
	 * @param monitor a progress monitor
	 * @return a status indicating the results of the merge
	 */
	protected IStatus performThreeWayMerge(final IThreeWayDiff diff, IProgressMonitor monitor) throws CoreException {
		final IStatus[] result = new IStatus[] { Status.OK_STATUS };
		run(monitor1 -> {
			monitor1.beginTask(null, 100);
			IResourceDiff localDiff = (IResourceDiff)diff.getLocalChange();
			IResourceDiff remoteDiff = (IResourceDiff)diff.getRemoteChange();
			IStorageMerger merger = getAdapter(IStorageMerger.class);
			if (merger == null) {
				merger = DelegatingStorageMerger.getInstance();
			}
			IFile file = (IFile)localDiff.getResource();
			monitor1.subTask(NLS.bind(Messages.MergeContext_5, file.getFullPath().toString()));
			String osEncoding = file.getCharset();
			IFileRevision ancestorState = localDiff.getBeforeState();
			IFileRevision remoteState = remoteDiff.getAfterState();
			IStorage ancestorStorage;
			if (ancestorState != null) {
				ancestorStorage = ancestorState.getStorage(Policy.subMonitorFor(monitor1, 30));
			} else {
				ancestorStorage = null;
			}
			IStorage remoteStorage = remoteState.getStorage(Policy.subMonitorFor(monitor1, 30));
			OutputStream os = getTempOutputStream(file);
			try {
				IStatus status = merger.merge(os, osEncoding, ancestorStorage, file, remoteStorage, Policy.subMonitorFor(monitor1, 30));
				if (status.isOK()) {
					file.setContents(getTempInputStream(file, os), false, true, Policy.subMonitorFor(monitor1, 5));
					markAsMerged(diff, false, Policy.subMonitorFor(monitor1, 5));
				} else {
					status = new MergeStatus(status.getPlugin(), status.getMessage(), new IFile[]{file});
				}
				result[0] = status;
			} finally {
				disposeTempOutputStream(file, os);
			}
			monitor1.done();
		}, getMergeRule(diff), IWorkspace.AVOID_UPDATE, monitor);
		return result[0];
	}

	private void disposeTempOutputStream(IFile file, OutputStream output) {
		if (output instanceof ByteArrayOutputStream) {
			return;
		}
		// We created a temporary file so we need to clean it up
		try {
			// First make sure the output stream is closed
			// so that file deletion will not fail because of that.
			if (output != null) {
				output.close();
			}
		} catch (IOException e) {
			// Ignore
		}
		File tmpFile = getTempFile(file);
		if (tmpFile.exists()) {
			tmpFile.delete();
		}
	}

	private OutputStream getTempOutputStream(IFile file) throws CoreException {
		File tmpFile = getTempFile(file);
		if (tmpFile.exists()) {
			tmpFile.delete();
		}
		File parent = tmpFile.getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
		}
		try {
			return new BufferedOutputStream(new FileOutputStream(tmpFile));
		} catch (FileNotFoundException e) {
			TeamPlugin.log(IStatus.ERROR, NLS.bind("Could not open temporary file {0} for writing: {1}", //$NON-NLS-1$
					tmpFile.getAbsolutePath(), e.getMessage()), e);
			return new ByteArrayOutputStream();
		}
	}

	private InputStream getTempInputStream(IFile file, OutputStream output) throws CoreException {
		if (output instanceof ByteArrayOutputStream baos) {
			return new ByteArrayInputStream(baos.toByteArray());
		}
		// We created a temporary file so we need to open an input stream on it
		try {
			// First make sure the output stream is closed
			if (output != null) {
				output.close();
			}
		} catch (IOException e) {
			// Ignore
		}
		File tmpFile = getTempFile(file);
		try {
			return new BufferedInputStream(new FileInputStream(tmpFile));
		} catch (FileNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, TeamPlugin.ID, IMergeStatus.INTERNAL_ERROR,
					NLS.bind(Messages.MergeContext_4, tmpFile.getAbsolutePath(), e.getMessage()), e));
		}
	}

	private File getTempFile(IFile file) {
		return TeamPlugin.getPlugin().getStateLocation().append(".tmp").append(file.getName() + ".tmp").toFile(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private IFile getLocalFile(IDiff delta) {
		return ResourcesPlugin.getWorkspace().getRoot().getFile(delta.getPath());
	}

	/**
	 * Make the local state of the resource associated with the given diff match
	 * that of the remote. This method is invoked by the
	 * {@link #merge(IDiff, boolean, IProgressMonitor)} method. By default, it
	 * either overwrites the local contexts with the remote contents if both
	 * exist, deletes the local if the remote does not exists or adds the local
	 * if the local doesn't exist but the remote does. It then calls
	 * {@link #makeInSync(IDiff, IProgressMonitor)} to give subclasses a change
	 * to make the file associated with the diff in-sync.
	 *
	 * @param diff
	 *            the diff whose local is to be replaced
	 * @param monitor
	 *            a progress monitor
	 * @throws CoreException if an error occurs
	 */
	protected void performReplace(final IDiff diff, IProgressMonitor monitor) throws CoreException {
		IResourceDiff d;
		IFile file = getLocalFile(diff);
		IFileRevision remote = null;
		if (diff instanceof IResourceDiff) {
			d = (IResourceDiff) diff;
			remote = d.getAfterState();
		} else {
			d = (IResourceDiff)((IThreeWayDiff)diff).getRemoteChange();
			if (d != null) {
				remote = d.getAfterState();
			}
		}
		if (d == null) {
			d = (IResourceDiff)((IThreeWayDiff)diff).getLocalChange();
			if (d != null) {
				remote = d.getBeforeState();
			}
		}

		// Only perform the replace if a local or remote change was found
		if (d != null) {
			performReplace(diff, file, remote, monitor);
		}
	}

	/**
	 * Method that is invoked from
	 * {@link #performReplace(IDiff, IProgressMonitor)} after the local has been
	 * changed to match the remote. Subclasses may override
	 * {@link #performReplace(IDiff, IProgressMonitor)} or this method in order
	 * to properly reconcile the synchronization state. This method is also
	 * invoked from {@link #merge(IDiff, boolean, IProgressMonitor)} if deletion
	 * conflicts are encountered. It can also be invoked from that same method if
	 * a folder is created due to an incoming folder addition.
	 *
	 * @param diff
	 *            the diff whose local is now in-sync
	 * @param monitor
	 *            a progress monitor
	 * @throws CoreException if an error occurs
	 */
	protected abstract void makeInSync(IDiff diff, IProgressMonitor monitor) throws CoreException;

	private void performReplace(final IDiff diff, final IFile file, final IFileRevision remote, IProgressMonitor monitor) throws CoreException {
		run(monitor1 -> {
			try {
				monitor1.beginTask(null, 100);
				monitor1.subTask(NLS.bind(Messages.MergeContext_6, file.getFullPath().toString()));
				if ((remote == null || !remote.exists()) && file.exists()) {
					file.delete(false, true, Policy.subMonitorFor(monitor1, 95));
				} else if (remote != null) {
					ensureParentsExist(file, monitor1);
					try (InputStream stream = new BufferedInputStream(remote.getStorage(monitor1).getContents())) {
						if (file.exists()) {
							file.setContents(stream, false, true, Policy.subMonitorFor(monitor1, 95));
						} else {
							file.create(stream, false, Policy.subMonitorFor(monitor1, 95));
						}
					} catch (IOException closeException) {
						// Ignore
					}
				}
				// Performing a replace should leave the file in-sync
				makeInSync(diff, Policy.subMonitorFor(monitor1, 5));
			} finally {
				monitor1.done();
			}
		}, getMergeRule(diff), IWorkspace.AVOID_UPDATE, monitor);
	}

	/**
	 * Ensure that the parent folders of the given resource exist.
	 * This method is invoked from {@link #performReplace(IDiff, IProgressMonitor)}
	 * for files that are being merged that do not exist locally.
	 * By default, this method creates the parents using
	 * {@link IFolder#create(boolean, boolean, IProgressMonitor)}.
	 * Subclasses may override.
	 * @param resource a resource
	 * @param monitor a progress monitor
	 * @throws CoreException if an error occurs
	 */
	protected void ensureParentsExist(IResource resource, IProgressMonitor monitor) throws CoreException {
		IContainer parent = resource.getParent();
		if (parent.getType() != IResource.FOLDER) {
			// this method will only create folders
			return;
		}
		if (!parent.exists()) {
			ensureParentsExist(parent, monitor);
			((IFolder)parent).create(false, true, monitor);
		}
	}

	/**
	 * Default implementation of <code>run</code> that invokes the
	 * corresponding <code>run</code> on {@link org.eclipse.core.resources.IWorkspace}.
	 * @see org.eclipse.team.core.mapping.IMergeContext#run(org.eclipse.core.resources.IWorkspaceRunnable, org.eclipse.core.runtime.jobs.ISchedulingRule, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void run(IWorkspaceRunnable runnable, ISchedulingRule rule, int flags, IProgressMonitor monitor) throws CoreException {
		ResourcesPlugin.getWorkspace().run(runnable, rule, flags, monitor);
	}

	/**
	 * Default implementation that returns the resource itself if it exists
	 * and the first existing parent if the resource does not exist.
	 * Subclass should override to provide the appropriate rule.
	 * @see org.eclipse.team.core.mapping.IMergeContext#getMergeRule(IDiff)
	 */
	@Override
	public ISchedulingRule getMergeRule(IDiff diff) {
		IResource resource = getDiffTree().getResource(diff);
		IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace().getRuleFactory();
		ISchedulingRule rule;
		if (!resource.exists()) {
			// for additions return rule for all parents that need to be created
			IContainer parent = resource.getParent();
			while (!parent.exists()) {
				resource = parent;
				parent = parent.getParent();
			}
			rule = ruleFactory.createRule(resource);
		} else if (SyncInfoToDiffConverter.getRemote(diff) == null){
			rule = ruleFactory.deleteRule(resource);
		} else {
			rule = ruleFactory.modifyRule(resource);
		}
		return rule;
	}

	@Override
	public ISchedulingRule getMergeRule(IDiff[] deltas) {
		ISchedulingRule result = null;
		for (IDiff node : deltas) {
			ISchedulingRule rule = getMergeRule(node);
			if (result == null) {
				result = rule;
			} else {
				result = MultiRule.combine(result, rule);
			}
		}
		return result;
	}

	@Override
	public int getMergeType() {
		return getType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IStorageMerger.class) {
			return (T) DelegatingStorageMerger.getInstance();
		}
		return super.getAdapter(adapter);
	}
}
