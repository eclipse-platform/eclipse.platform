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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.LogEntry;
import org.eclipse.team.internal.ccvs.core.client.listeners.LogListener;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderTree;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.model.RemoteContentProvider;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation.LogEntryCache;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoriesView;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IWorkbenchPart;

public class FetchAllMembersOperation extends RemoteOperation {
	
	class RLogTreeBuilder {
			
			private ICVSRepositoryLocation location;
			private RemoteFolderTree tree;
			private CVSTag tag;

			public RLogTreeBuilder(ICVSRepositoryLocation location, CVSTag tag) {
				this.tag = tag;
				this.location = location;
				reset();
			}
			
			public RemoteFolderTree getTree() {
				return tree;
			}
		
			/**
			 * Reset the builder to prepare for a new build
			 */
			public void reset() {
				tree = new RemoteFolderTree(null, location, ICVSRemoteFolder.REPOSITORY_ROOT_FOLDER_NAME, tag);
				tree.setChildren(new ICVSRemoteResource[0]);
			}
			
		/*
		 * @see
		 * org.eclipse.team.internal.ccvs.core.client.listeners.RDiffSummaryListener.
		 * IFileDiffListener#newFile(java.lang.String, java.lang.String)
		 */
			public void newFile(IPath remoteFilePath, ICVSRemoteFile remoteFile) {
				try {
					addFile(tree,tag,remoteFile, remoteFilePath);
				} catch (CVSException e) {
					CVSUIPlugin.log(e);
				} 
			}
			
			private void addFile(RemoteFolderTree tree, CVSTag tag, ICVSRemoteFile file,IPath filePath) throws CVSException {
				RemoteFolderTree parent = (RemoteFolderTree)getFolder(tree, tag, filePath.removeLastSegments(1), Path.EMPTY);
				addChild(parent, file);
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
	}
	
	static final String DEAD_STATE = "dead"; //$NON-NLS-1$
	ICVSRepositoryLocation repoLocation;
	
	public FetchAllMembersOperation(IWorkbenchPart part, ICVSRemoteResource[] folders, ICVSRepositoryLocation repoLocation) {
		super(part, folders);
		this.repoLocation = repoLocation;
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
		//expand each folder selected in the tree
			ICVSRemoteResource[] restest = getRemoteResources();
			ICVSRemoteFolder testfolder = (ICVSRemoteFolder) restest[0];
			CVSTag tag = testfolder.getTag();
			if (tag == null)
				tag = CVSTag.DEFAULT;
			LogEntryCache cache = new LogEntryCache();

			RemoteLogOperation operation = new RemoteLogOperation(getPart(), getRemoteResources(), tag, null, cache);
			try {
				operation.run(monitor);	
				ICVSRemoteResource[] remoteRes = getRemoteResources(); 
				final ICVSRemoteFolder project = (ICVSRemoteFolder) remoteRes[0];
				//Get the entry paths
				String[] entry = cache.getCachedFilePaths();
				//Strip repo + project info from entries
				RLogTreeBuilder treeBuilder = new RLogTreeBuilder(project.getRepository(),tag);
				for (String e : entry) {
					ILogEntry[] logEntry = cache.getLogEntries(e);
					//might not have state if this a branch entry
					if (logEntry[0].getState() != null &&
							logEntry[0].getState().equals(DEAD_STATE))
						continue;
					ICVSRemoteFile remoteFile = logEntry[0].getRemoteFile();
					//if the current folder tag is a branch tag, we need to take the extra step
					//of making sure that the file's revision number has been set appropriately
					if (tag.getType() == CVSTag.BRANCH &&
							remoteFile.getRevision().equals(LogListener.BRANCH_REVISION))
						verifyRevision(tag, logEntry[0], remoteFile);
					IPath logPath = new Path(null,remoteFile.getRepositoryRelativePath());
					if (logPath.segmentCount()>0)
						logPath = logPath.removeFirstSegments(1);
				
					treeBuilder.newFile(logPath, remoteFile);
				}
			
				RemoteFolderTree remoteTree = treeBuilder.getTree();
				IWorkbenchPart part = this.getPart();
				if (part instanceof RepositoriesView ){
					final RepositoriesView repView = (RepositoriesView) part;
					RemoteContentProvider prov = repView.getContentProvider();
					prov.addCachedTree(project, remoteTree);
					final TreeViewer tree = repView.getViewer();
				
				Utils.asyncExec((Runnable) () -> tree.expandToLevel(project, AbstractTreeViewer.ALL_LEVELS),
						repView.getViewer());
				}
				
			} catch (InvocationTargetException e) {
				throw CVSException.wrapException(e);
			} catch (InterruptedException e) {
				// Ignore;
			} catch (TeamException e){
				throw CVSException.wrapException(e);
			}
	
	}

	private void verifyRevision(CVSTag tag, ILogEntry entry, ICVSRemoteFile remoteFile) throws CVSException {
		if (entry instanceof LogEntry){
			LogEntry logEntry = (LogEntry) entry;
			String[] allBranchRevisions = logEntry.getBranchRevisions();
			CVSTag[] allCVSTags = entry.getTags();
			for (int i = 0; i < allCVSTags.length; i++) {
				if (allCVSTags[i].equals(tag)){
					//get the revision number stored for this tag
					((RemoteFile) remoteFile).setRevision(allBranchRevisions[i]);
					break;
				}
			}
		}	
	}
	
	@Override
	protected String getTaskName() {
		return CVSUIMessages.FetchAllMembersOperation_0;
	}


}
