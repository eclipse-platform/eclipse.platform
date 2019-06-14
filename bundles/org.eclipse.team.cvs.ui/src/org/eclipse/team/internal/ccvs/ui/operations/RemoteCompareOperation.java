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

import java.util.*;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.listeners.RDiffSummaryListener;
import org.eclipse.team.internal.ccvs.core.resources.*;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Compare the two versions of given remote folders obtained from the two tags specified.
 */
public class RemoteCompareOperation extends RemoteOperation {
	
	private CompareTreeBuilder builder;
	private CVSTag left, right;
	
	/**
	 * Helper class for builder and comparing the resource trees
	 */
	public static class CompareTreeBuilder implements RDiffSummaryListener.IFileDiffListener {
		private ICVSRepositoryLocation location;
		private RemoteFolderTree leftTree, rightTree;
		private CVSTag left, right;

		public CompareTreeBuilder(ICVSRepositoryLocation location, CVSTag left, CVSTag right) {
			this.left = left;
			this.right = right;
			this.location = location;
			reset();
		}
		
		public RemoteFolderTree getLeftTree() {
			return leftTree;
		}
		public RemoteFolderTree getRightTree() {
			return rightTree;
		}
		
		/**
		 * Reset the builder to prepare for a new build
		 */
		public void reset() {
			leftTree = new RemoteFolderTree(null, location, ICVSRemoteFolder.REPOSITORY_ROOT_FOLDER_NAME, left);
			leftTree.setChildren(new ICVSRemoteResource[0]);
			rightTree = new RemoteFolderTree(null, location, ICVSRemoteFolder.REPOSITORY_ROOT_FOLDER_NAME, right);
			rightTree.setChildren(new ICVSRemoteResource[0]);
		}
		
		/**
		 * Cache the contents for the files that are about to be compares
		 * @throws CVSException
		 */
		public void cacheContents(IProgressMonitor monitor) throws CVSException {
			String[] overlappingFilePaths = getOverlappingFilePaths();
			if (overlappingFilePaths.length > 0) {
				monitor.beginTask(null, 100);
				fetchFileContents(leftTree, overlappingFilePaths, Policy.subMonitorFor(monitor, 50));
				fetchFileContents(rightTree, overlappingFilePaths, Policy.subMonitorFor(monitor, 50));
				monitor.done();
			}
		}
		
		/**
		 * Open the comparison in a compare editor
		 */
		public void openCompareEditor(final IWorkbenchPage page, final String title, final String toolTip) {
			if (leftTree == null || rightTree == null) return;
			Display.getDefault().asyncExec(() -> CompareUI.openCompareEditorOnPage(new CVSCompareEditorInput(title,
					toolTip, new ResourceEditionNode(leftTree), new ResourceEditionNode(rightTree)), page));
		}
		
		/**
		 * Add the predecessor to the left tree and the remote to the right tree.
		 * @param predecessor
		 * @param remote
		 */
		public void addToTrees(ICVSRemoteFile predecessor, ICVSRemoteFile remote) {
			if (remote != null) {
				try {
					Path filePath = new Path(null, remote.getRepositoryRelativePath());
					addFile(rightTree, right, filePath, remote.getRevision());
					getFolder(leftTree, left, filePath.removeLastSegments(1), Path.EMPTY);
				} catch (TeamException e) {
					CVSUIPlugin.log(e);
				}
			}
			if (predecessor != null) {
				try {
					Path filePath = new Path(null, predecessor.getRepositoryRelativePath());
					addFile(leftTree, left, filePath, predecessor.getRevision());
					getFolder(rightTree, right, filePath.removeLastSegments(1), Path.EMPTY);
				} catch (TeamException e) {
					CVSUIPlugin.log(e);
				}
			}
		}
		
		private void addFile(RemoteFolderTree tree, CVSTag tag, Path filePath, String revision) throws CVSException {
			RemoteFolderTree parent = (RemoteFolderTree)getFolder(tree, tag, filePath.removeLastSegments(1), Path.EMPTY);
			String name = filePath.lastSegment();
			ICVSRemoteFile file = new RemoteFile(parent, 0, name, revision, null, getTag(revision, tag));
			addChild(parent, file);
		}
		
		private CVSTag getTag(String revision, CVSTag tag) {
			if (tag == null) {
				tag = new CVSTag(revision, CVSTag.VERSION);
			}
			return tag;
		}

		/* 
		 * Get the folder at the given path in the given tree, creating any missing folders as needed.
		 */
		private ICVSRemoteFolder getFolder(RemoteFolderTree tree, CVSTag tag, IPath remoteFolderPath, IPath parentPath) throws CVSException {
			if (remoteFolderPath.segmentCount() == 0) return tree;
			String name = remoteFolderPath.segment(0);
			ICVSResource child;
			IPath childPath = parentPath.append(name);
			if (tree.childExists(name)) {
				child = tree.getChild(name);
			}  else {
				child = new RemoteFolderTree(tree, tree.getRepository(), childPath.toString(), tag);
				((RemoteFolderTree)child).setChildren(new ICVSRemoteResource[0]);
				addChild(tree, (ICVSRemoteResource)child);
			}
			return getFolder((RemoteFolderTree)child, tag, remoteFolderPath.removeFirstSegments(1), childPath);
		}

		private void addChild(RemoteFolderTree tree, ICVSRemoteResource resource) {
			ICVSRemoteResource[] children = tree.getChildren();
			ICVSRemoteResource[] newChildren;
			if (children == null) {
				newChildren = new ICVSRemoteResource[] { resource };
			} else {
				newChildren = new ICVSRemoteResource[children.length + 1];
				System.arraycopy(children, 0, newChildren, 0, children.length);
				newChildren[children.length] = resource;
			}
			tree.setChildren(newChildren);
		}
		
		@Override
		public void fileDiff(String remoteFilePath, String leftRevision, String rightRevision) {
			try {
				addFile(rightTree, right, new Path(null, remoteFilePath), rightRevision);
			} catch (CVSException e) {
				CVSUIPlugin.log(e);
			}
			try {
				addFile(leftTree, left, new Path(null, remoteFilePath), leftRevision);
			} catch (CVSException e) {
				CVSUIPlugin.log(e);
			}
		}

		@Override
		public void newFile(String remoteFilePath, String rightRevision) {
			try {
				addFile(rightTree, right, new Path(null, remoteFilePath), rightRevision);
			} catch (CVSException e) {
				CVSUIPlugin.log(e);
			}
		}

		@Override
		public void deletedFile(String remoteFilePath, String leftRevision) {
			// The leftRevision may be null in which case the tag is used
			try {
				addFile(leftTree, left, new Path(null, remoteFilePath), leftRevision);
			} catch (CVSException e) {
				CVSUIPlugin.log(e);
			}
		}

		@Override
		public void directory(String remoteFolderPath) {
			try {
				getFolder(leftTree, left, new Path(null, remoteFolderPath), Path.EMPTY);
			} catch (CVSException e) {
				CVSUIPlugin.log(e);
			}
			try {
				getFolder(rightTree, right, new Path(null, remoteFolderPath), Path.EMPTY);
			} catch (CVSException e) {
				CVSUIPlugin.log(e);
			}
		}
		
		private String[] getOverlappingFilePaths() {
			String[] leftFiles = getFilePaths(leftTree);
			String[] rightFiles = getFilePaths(rightTree);
			Set<String> set = new HashSet<>();
			for (String rightFile : rightFiles) {
				for (String leftFile : leftFiles) {
					if (leftFile.equals(rightFile)) {
						set.add(leftFile);
					}
				}
			}
			return set.toArray(new String[set.size()]);
		}

		private void fetchFileContents(RemoteFolderTree tree, String[] overlappingFilePaths, IProgressMonitor monitor) throws CVSException {
			FileContentCachingService.fetchFileContents(tree, overlappingFilePaths, monitor);
		}

		private String[] getFilePaths(RemoteFolderTree tree) {
			ICVSRemoteResource[] children = tree.getChildren();
			List<String> result = new ArrayList<>();
			for (ICVSRemoteResource resource : children) {
				if (resource.isContainer()) {
					result.addAll(Arrays.asList(getFilePaths((RemoteFolderTree)resource)));
				} else {
					result.add(resource.getRepositoryRelativePath());
				}
			}
			return result.toArray(new String[result.size()]);
		}
	}
	
	public static CVSTag getTag(ICVSRemoteResource resource) throws CVSException {
		CVSTag tag = null;
		try {
			if (resource.isContainer()) {
				tag = ((ICVSRemoteFolder)resource).getTag();
			} else {
				ICVSRemoteFile file = (ICVSRemoteFile)resource;
				String revision = file.getRevision();
				if (revision.equals(ResourceSyncInfo.ADDED_REVISION)) {
					ResourceSyncInfo info =file.getSyncInfo();
					if (info != null) tag = info.getTag();
				} else {
					tag = new CVSTag(revision, CVSTag.VERSION);
				}
			}
		} catch (TeamException e) {
			throw CVSException.wrapException(e);
		}
		if (tag == null) tag = CVSTag.DEFAULT;
		return tag;
	}
	
	public static RemoteCompareOperation create(IWorkbenchPart part, ICVSRemoteResource remoteResource, CVSTag tag) throws CVSException {
		CVSTag tag0 = getTag(remoteResource);
		CVSTag tag1 = tag;
		if (tag0.getType() == CVSTag.DATE && tag1.getType() == CVSTag.DATE) {
			if (tag0.asDate().after(tag1.asDate())) {
				tag = tag0;
				remoteResource = remoteResource.forTag(tag1);
			}
		}
		return new RemoteCompareOperation(part, remoteResource, tag);
	}
	
	/**
	 * Compare two versions of the given remote resource.
	 * @param shell
	 * @param remoteResource the resource whose tags are being compared
	 * @param left the earlier tag (not null)
	 * @param right the later tag (not null)
	 */
	protected RemoteCompareOperation(IWorkbenchPart part, ICVSRemoteResource remoteResource, CVSTag tag) {
		super(part, new ICVSRemoteResource[] {remoteResource});
		Assert.isNotNull(tag);
		this.right = tag;
		try {
			this.left = getTag(remoteResource);
		} catch (CVSException e) {
			// This shouldn't happen but log it just in case
			CVSProviderPlugin.log(e);
		}
		if (this.left == null) {
			this.left = CVSTag.DEFAULT;
		}
		builder = new CompareTreeBuilder(remoteResource.getRepository(), left, right);
	}

	/*
	 * This command only supports the use of a single resource
	 */
	private ICVSRemoteResource getRemoteResource() {
		return getRemoteResources()[0];
	}
	
	@Override
	protected void execute(IProgressMonitor monitor) throws CVSException {
		boolean fetchContents = CVSUIPlugin.getPlugin().getPluginPreferences().getBoolean(ICVSUIConstants.PREF_CONSIDER_CONTENTS);
		monitor.beginTask(getTaskName(), 50 + (fetchContents ? 100 : 0));
		try {
			ICVSRemoteResource resource = getRemoteResource();
			IStatus status = buildTrees(resource, Policy.subMonitorFor(monitor, 50));
			if (status.isOK() && fetchContents) {
				builder.cacheContents(Policy.subMonitorFor(monitor, 100));
			}
			collectStatus(status);
			openCompareEditor(builder);
		} finally {
			monitor.done();
		}
	}

	/**
	 * This method is here to allow subclasses to override
	 */
	protected void openCompareEditor(CompareTreeBuilder builder) {
		builder.openCompareEditor(getTargetPage(), null, null);
	}

	/*
	 * Build the two trees uses the reponses from "cvs rdiff -s ...".
	 */
	private IStatus buildTrees(ICVSRemoteResource resource, IProgressMonitor monitor) throws CVSException {
		// Initialize the resulting trees
		builder.reset();
		Command.QuietOption oldOption= CVSProviderPlugin.getPlugin().getQuietness();
		Session session = new Session(resource.getRepository(), builder.getLeftTree(), false);
		try {
			monitor.beginTask(getTaskName(), 100);
			CVSProviderPlugin.getPlugin().setQuietness(Command.VERBOSE);
			session.open(Policy.subMonitorFor(monitor, 10));
			IStatus status = Command.RDIFF.execute(session,
					Command.NO_GLOBAL_OPTIONS,
					getLocalOptions(),
					new ICVSResource[] { resource },
					new RDiffSummaryListener(builder),
					Policy.subMonitorFor(monitor, 90));
			return status;
		} finally {
			try {
				session.close();
			} finally {
				CVSProviderPlugin.getPlugin().setQuietness(oldOption);
			}
			monitor.done();
		}
	}

	private LocalOption[] getLocalOptions() {
		return new LocalOption[] {RDiff.SUMMARY, RDiff.makeTagOption(left), RDiff.makeTagOption(right)};
	}

	@Override
	protected String getTaskName() {
		return NLS.bind(CVSUIMessages.RemoteCompareOperation_0, (new Object[] {left.getName(), right.getName(), getRemoteResource().getRepositoryRelativePath()})); 
	}
	
	protected IWorkbenchPage getTargetPage() {
		return TeamUIPlugin.getActivePage();
	}
}
