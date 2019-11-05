/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.resources;

import java.util.*;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.*;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Util;

/*
 * This class is responsible for building a remote tree that shows the repository
 * state of a locally loaded folder tree.
 * 
 * It is used as follows
 * 
 * 		RemoteFolderTreeBuilder.buildRemoteTree(CVSRepositoryLocation, IManagedFolder, String, IProgressMonitor);
 * 
 * The provider IManagedFolder can be a local resource or a RemoteFolderTree that
 * that was previously built.
 */
public class RemoteFolderTreeBuilder {

	private static final int MAX_REVISION_FETCHES_PER_CONNECTION = 1024;
	
	private Map<String, Map<String, DeltaNode>> fileDeltas;
	private List<String> changedFiles;
	private Map<String, RemoteFolderTree> remoteFolderTable;
	
	private ICVSFolder root;
	private RemoteFolderTree remoteRoot;
	private CVSRepositoryLocation repository;
	
	private CVSTag tag;
	
	private LocalOption[] updateLocalOptions;
	
	private boolean rootDoesNotExist = false;
	
	private static String UNKNOWN = ""; //$NON-NLS-1$
	private static String DELETED = "DELETED"; //$NON-NLS-1$
	private static String ADDED = "ADDED"; //$NON-NLS-1$
	private static String FOLDER = "FOLDER"; //$NON-NLS-1$
	
	private static Map EMPTY_MAP = new HashMap();
	
	private boolean newFolderExist = false;
	
	static class DeltaNode {
		int syncState = Update.STATE_NONE;
		String name;
		String revision;
		
		DeltaNode(String name, String revision, int syncState) {
			this.name = name;
			this.revision = revision;
			this.syncState = syncState;			
		}
		
		String getName() {
			return name;
		}
		
		String getRevision() {
			return revision;
		}
		
		int getSyncState() {
			return syncState;
		}
	}
		
	
	/* package */ RemoteFolderTreeBuilder(CVSRepositoryLocation repository, ICVSFolder root, CVSTag tag) {
		this.repository = repository;
		this.root = root;
		this.tag = tag;
		this.fileDeltas = new HashMap<>();
		this.changedFiles = new ArrayList<>();
		this.remoteFolderTable = new HashMap<>();
		
		// Build the local options
		List<LocalOption> localOptions = new ArrayList<>();
		if (tag != null) {
			if (tag.getType() == CVSTag.HEAD) {
				localOptions.add(Update.CLEAR_STICKY);
			} else {
				localOptions.add(Update.makeTagOption(tag));
			}
		}
		updateLocalOptions = localOptions.toArray(new LocalOption[localOptions.size()]);
	}
	
	private LocalOption[] getOptionsWithoutTag() {
		// Build the local options
		List<LocalOption> localOptions = new ArrayList<>();
		localOptions.add(Update.RETRIEVE_ABSENT_DIRECTORIES);
		return localOptions.toArray(new LocalOption[localOptions.size()]);
	}
	
	public static RemoteFolder buildBaseTree(CVSRepositoryLocation repository, ICVSFolder root, CVSTag tag, IProgressMonitor progress) throws CVSException {
		try {
			RemoteFolderTreeBuilder builder = new RemoteFolderTreeBuilder(repository, root, tag);
			progress.beginTask(null, 100);
			IProgressMonitor subProgress = Policy.infiniteSubMonitorFor(progress, 100);
			subProgress.beginTask(null, 512);  
			subProgress.subTask(NLS.bind(CVSMessages.RemoteFolderTreeBuilder_buildingBase, new String[] { root.getName() })); 
			return builder.buildBaseTree(null, root, subProgress);
		} finally {
			progress.done();
		}
	}
	
	public static RemoteFolderTree buildRemoteTree(CVSRepositoryLocation repository, IContainer root, CVSTag tag, IProgressMonitor monitor) throws CVSException {
		return buildRemoteTree(repository, CVSWorkspaceRoot.getCVSFolderFor(root), tag, monitor);
	}
	
	public static RemoteFolderTree buildRemoteTree(CVSRepositoryLocation repository, ICVSFolder root, CVSTag tag, IProgressMonitor monitor) throws CVSException {
		RemoteFolderTreeBuilder builder = new RemoteFolderTreeBuilder(repository, root, tag);
		return builder.buildTree(new ICVSResource[] { root }, monitor);
	}
	public static RemoteFile buildRemoteTree(CVSRepositoryLocation repository, ICVSFile file, CVSTag tag, IProgressMonitor monitor) throws CVSException {
		RemoteFolderTreeBuilder builder = new RemoteFolderTreeBuilder(repository, file.getParent(), tag);
		return builder.buildTree(file, monitor);
	}
	
	/* package */ RemoteFolderTree buildTree(ICVSResource[] resources, IProgressMonitor monitor) throws CVSException {
		
		// Make sure that the cvs commands are not quiet during this operations
		QuietOption quietness = CVSProviderPlugin.getPlugin().getQuietness();
		try {
			CVSProviderPlugin.getPlugin().setQuietness(Command.VERBOSE);
			
			monitor.beginTask(null, 100);

			// 1st Connection: Use local state to determine delta with server
			if (!fetchDelta(resources, Policy.subMonitorFor(monitor, 75))) {
				return null;
			}
			
			// 2nd Connection: Build remote tree from above delta using 2nd connection to fetch unknown directories
			// NOTE: Multiple commands may be issued over this connection.
			fetchNewDirectories(Policy.subMonitorFor(monitor, 10));

			//	3rd+ Connection: Used to fetch file status in groups of 1024
			fetchFileRevisions(Policy.subMonitorFor(monitor, 15));
			
			return remoteRoot;
			
		} finally {
			CVSProviderPlugin.getPlugin().setQuietness(quietness);
			monitor.done();
		}
	}

	private boolean fetchDelta(ICVSResource[] resources, IProgressMonitor monitor) throws CVSException {
		
		// Get the arguments from the files
		ArrayList<String> arguments = new ArrayList<>();
		for (ICVSResource resource : resources) {
			arguments.add(resource.getRelativePath(root));
		}
		
		// Use local state to determine delta with server
		monitor.beginTask(null, 100);
		Policy.checkCanceled(monitor);
		Session session = new Session(repository, root, false);
		session.open(Policy.subMonitorFor(monitor, 10), false /* read-only */);
		try {
			Policy.checkCanceled(monitor);
			fetchDelta(session, arguments.toArray(new String[arguments.size()]), Policy.subMonitorFor(monitor, 90));
			if (rootDoesNotExist) {
				// We cannot handle the case where a project (i.e. the top-most CVS folder)
				// has been deleted directly on the sever (i.e. deleted using rm -rf)
				if (root.isCVSFolder() && ! root.isManaged()) {
					IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSMessages.RemoteFolderTreeBuild_folderDeletedFromServer, new String[] { root.getFolderSyncInfo().getRepository() }),root);
					throw new CVSException(status); 
				} else {
					return false;
				}
			}
		} finally {
			session.close();
			monitor.done();
		}
		return true;
	}

	private void fetchNewDirectories(IProgressMonitor monitor) throws CVSException {
		// Build remote tree from the fetched delta using a new connection to fetch unknown directories
		// NOTE: Multiple commands may be issued over this connection.
		monitor.beginTask(null, 100);
		Session session;
		FolderSyncInfo folderSyncInfo = root.getFolderSyncInfo();
		if (folderSyncInfo == null) {
			// We've lost the mapping in the local workspace.
			// This could be due to the project being deleted.
			if (root.exists()) {
				IResource resource = root.getIResource();
				String path;
				if (resource == null) {
					path = root.getName();
				} else {
					path = resource.getFullPath().toString();
				}
				IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSMessages.RemoteFolderTreeBuilder_0, new String[] { path }), root);
				throw new CVSException(status); 
			} else {
				// Just return. The remote tree will be null
				return;
			}
		}
		remoteRoot =
			new RemoteFolderTree(null, root.getName(), repository,
				folderSyncInfo.getRepository(),
				tagForRemoteFolder(root, tag));
		if (newFolderExist) {
			// New folders will require a connection for fetching their members
			session = new Session(repository, remoteRoot, false);
			session.open(Policy.subMonitorFor(monitor, 10), false /* read-only */);
		} else {
			session = null;
		}
		try {
			// Set up an infinite progress monitor for the recursive build
			IProgressMonitor subProgress = Policy.infiniteSubMonitorFor(monitor, 90);
			subProgress.beginTask(null, 512);
			// Build the remote tree
			buildRemoteTree(session, root, remoteRoot, "", subProgress); //$NON-NLS-1$
		} finally {
			if (session != null) {
				session.close();
			}
			monitor.done();
		}
	}
	
	private void fetchFileRevisions(IProgressMonitor monitor) throws CVSException {
		// 3rd+ Connection: Used to fetch file status in groups of 1024
		if (remoteRoot != null && !changedFiles.isEmpty()) {
			String[] allChangedFiles = changedFiles.toArray(new String[changedFiles.size()]);
			int iterations = (allChangedFiles.length / MAX_REVISION_FETCHES_PER_CONNECTION) 
				+ (allChangedFiles.length % MAX_REVISION_FETCHES_PER_CONNECTION == 0 ? 0 : 1);
			for (int i = 0; i < iterations ; i++) {
				int length = Math.min(MAX_REVISION_FETCHES_PER_CONNECTION, 
					allChangedFiles.length - (MAX_REVISION_FETCHES_PER_CONNECTION * i));
				String buffer[] = new String[length];
				System.arraycopy(allChangedFiles, i * MAX_REVISION_FETCHES_PER_CONNECTION, buffer, 0, length);
				Session session = new Session(repository, remoteRoot, false);
				session.open(Policy.subMonitorFor(monitor, 1), false /* read-only */);
				try {
					fetchFileRevisions(session, buffer, Policy.subMonitorFor(monitor, 2));
				} finally {
					session.close();
				}
			}
		}
	}
	
	/* package */ RemoteFile buildTree(ICVSFile file, IProgressMonitor monitor) throws CVSException {
		QuietOption quietness = CVSProviderPlugin.getPlugin().getQuietness();
		try {
			CVSProviderPlugin.getPlugin().setQuietness(Command.VERBOSE);
			
			monitor.beginTask(null, 100);
	
			// Query the server to see if there is a delta available
			Policy.checkCanceled(monitor);
			Session session = new Session(repository, root, false);
			session.open(Policy.subMonitorFor(monitor, 10), false /* read-only */);
			try {
				Policy.checkCanceled(monitor);
				fetchDelta(session, new String[] { file.getName() }, Policy.subMonitorFor(monitor, 50));
				if (rootDoesNotExist) {
					return null;
				}
			} finally {
				session.close();
			}
			// Create a parent for the remote resource
			remoteRoot =
				new RemoteFolderTree(null, root.getName(), repository,
					root.getFolderSyncInfo().getRepository(),
					tagForRemoteFolder(root, tag));
			// Create the remote resource (using the delta if there is one)
			RemoteFile remoteFile;
			Map deltas = fileDeltas.get(""); //$NON-NLS-1$
			if (deltas == null || deltas.isEmpty()) {
				// If the file is an addition, return null as the remote
				// Note: If there was a conflicting addition, the delta would not be empty
				byte[] syncBytes = file.getSyncBytes();
				if ( syncBytes == null || ResourceSyncInfo.isAddition(syncBytes)) {
					return null;
				}
				remoteFile = new RemoteFile(remoteRoot, syncBytes);
			} else {
				DeltaNode d = (DeltaNode)deltas.get(file.getName());
				if (d.getRevision() == DELETED) {
					return null;
				}
				CVSTag newTag = tagForRemoteFolder(remoteRoot, tag);
				if (newTag == null && file.getSyncInfo() != null) {
					newTag = file.getSyncInfo().getTag();
				}
				remoteFile = new RemoteFile(remoteRoot, 
					d.getSyncState(), 
					file.getName(), 
					null, /* the revision will be retrieved from the server */
					getKeywordMode(file), /* use the same keyword mode as the local file */
					newTag);
			}
			// Add the resource to its parent
			remoteRoot.setChildren(new ICVSRemoteResource[] {remoteFile});
			// If there was a delta, fetch the new revision
			if (!changedFiles.isEmpty()) {
				// Add the remote folder to the remote folder lookup table (used to update file revisions)
				recordRemoteFolder(remoteRoot);
				session = new Session(repository, remoteRoot, false);
				session.open(Policy.subMonitorFor(monitor, 10), false /* read-only */);
				try {
					fetchFileRevisions(session, changedFiles.toArray(new String[changedFiles.size()]), Policy.subMonitorFor(monitor, 20));
				} finally {
					session.close();
				}
			}
			return remoteFile;
			
		} finally {
			CVSProviderPlugin.getPlugin().setQuietness(quietness);
			monitor.done();
		}
	}
	
	private Command.KSubstOption getKeywordMode(ICVSFile file) throws CVSException {
		if (file == null) return null;
		byte[] syncBytes = file.getSyncBytes();
		if (syncBytes == null) return null;
		return ResourceSyncInfo.getKeywordMode(syncBytes);
	}

	/*
	 * Build the base remote tree from the local tree.
	 * 
	 * The localPath is used to retrieve deltas from the recorded deltas
	 * 
	 * Does 1 work for each managed file and folder
	 */
	RemoteFolder buildBaseTree(RemoteFolder parent, ICVSFolder local, IProgressMonitor monitor) throws CVSException {
		
		Policy.checkCanceled(monitor);
					
		// Create a remote folder tree corresponding to the local resource
		FolderSyncInfo folderSyncInfo = local.getFolderSyncInfo();
		if (folderSyncInfo == null) return null;
		RemoteFolder remote = createRemoteFolder(local, parent, folderSyncInfo);

		// Create a List to contain the created children
		List<RemoteResource> children = new ArrayList<>();
		
		// Build the child folders corresponding to local folders base
		ICVSResource[] folders = local.members(ICVSFolder.FOLDER_MEMBERS);
		for (ICVSResource f : folders) {
			ICVSFolder folder = (ICVSFolder) f;
			if (folder.isManaged() && folder.isCVSFolder()) {
				monitor.worked(1);
				RemoteFolder tree = buildBaseTree(remote, folder, monitor);
				if (tree != null)
					children.add(tree);
			}
		}
		
		// Build the child files corresponding to local files base
		ICVSResource[] files = local.members(ICVSFolder.FILE_MEMBERS);
		for (ICVSResource f : files) {
			ICVSFile file = (ICVSFile) f;
			byte[] syncBytes = file.getSyncBytes();
			// if there is no sync info then there is no base
			if (syncBytes==null)
				continue;
			// There is no remote if the file was added
			if (ResourceSyncInfo.isAddition(syncBytes))
				continue;
			// If the file was deleted locally, we need to generate a new sync info without the delete flag
			if (ResourceSyncInfo.isDeletion(syncBytes)) {
				syncBytes = ResourceSyncInfo.convertFromDeletion(syncBytes);
			}
			children.add(createRemoteFile(remote, syncBytes));
			monitor.worked(1);
		}
		
		// Remove any folders that are phantoms locally if they have no children
		if (children.isEmpty() && isPruneEmptyDirectories() && !local.exists())
			return null;

		// Add the children to the remote folder tree
		remote.setChildren(children.toArray(new ICVSRemoteResource[children.size()]));
		
		return remote;
	}

	protected RemoteFile createRemoteFile(RemoteFolder remote, byte[] syncBytes) throws CVSException {
		return new RemoteFile(remote, syncBytes);
	}

	protected RemoteFolder createRemoteFolder(ICVSFolder local, RemoteFolder parent, FolderSyncInfo folderSyncInfo) {
		return new RemoteFolderTree(parent, local.getName(), repository, folderSyncInfo.getRepository(), folderSyncInfo.getTag());
	}
	
	/*
	 * Build the remote tree from the local tree and the recorded deltas.
	 * 
	 * The localPath is used to retrieve deltas from the recorded deltas
	 * 
	 * Does 1 work for each file and folder delta processed
	 */
	private void buildRemoteTree(Session session, ICVSFolder local, RemoteFolderTree remote, String localPath, IProgressMonitor monitor) throws CVSException {
		
		Policy.checkCanceled(monitor);
		
		// Add the remote folder to the remote folder lookup table (used to update file revisions)
		recordRemoteFolder(remote);
		
		// Create a map to contain the created children
		Map<String, RemoteResource> children = new HashMap<>();
		
		// If there's no corresponding local resource then we need to fetch its contents in order to populate the deltas
		if (local == null) {
			fetchNewDirectory(session, remote, localPath, monitor);
		}
		
		// Fetch the delta's for the folder
		Map deltas = fileDeltas.get(localPath);
		if (deltas == null)
			deltas = EMPTY_MAP;
		
		// If there is a local, use the local children to start building the remote children
		if (local != null) {
			// Build the child folders corresponding to local folders
			ICVSResource[] folders = local.members(ICVSFolder.FOLDER_MEMBERS);
			for (ICVSResource f : folders) {
				ICVSFolder folder = (ICVSFolder) f;
				DeltaNode d = (DeltaNode)deltas.get(folder.getName());
				if (folder.isCVSFolder() && ! isOrphanedSubtree(folder) && (d==null || d.getRevision() != DELETED)) {
					children.put(f.getName(), new RemoteFolderTree(remote, f.getName(), repository, folder.getFolderSyncInfo().getRepository(), tagForRemoteFolder(folder,tag)));
				}
			}
			// Build the child files corresponding to local files
			ICVSResource[] files = local.members(ICVSFolder.FILE_MEMBERS);
			for (ICVSResource f : files) {
				ICVSFile file = (ICVSFile) f;
				DeltaNode d = (DeltaNode)deltas.get(file.getName());
				byte[] syncBytes = file.getSyncBytes();
				// if there is no sync info then there isn't a remote file for this local file on the
				// server.
				if (syncBytes==null)
					continue;
				// There is no remote if the file was added and we didn't get a conflict (C) indicator from the server
				if (ResourceSyncInfo.isAddition(syncBytes) && d==null)
					continue;
				// There is no remote if the file was deleted and we didn't get a remove (R) indicator from the server
				if (ResourceSyncInfo.isDeletion(syncBytes) && d==null)
					continue;
					
				int type = d==null ? Update.STATE_NONE : d.getSyncState();
				children.put(file.getName(), new RemoteFile(remote, type, syncBytes));
			}
		}
		
		// Build the children for new or out-of-date resources from the deltas
		Iterator i = deltas.keySet().iterator();
		while (i.hasNext()) {
			String name = (String)i.next();
			DeltaNode d = (DeltaNode)deltas.get(name);
			String revision = d.getRevision();
			if (revision == FOLDER) {
				children.put(name, new RemoteFolderTree(remote, repository, 
					Util.appendPath(remote.getRepositoryRelativePath(), name), 
					tagForRemoteFolder(remote, tag)));
			} else if (revision == ADDED) {
				children.put(name, new RemoteFile(remote, 
					d.getSyncState(), 
					name, 
					null, /* the revision will be fetched later */
					null, /* there's no way to know the remote keyword mode */
					tagForRemoteFolder(remote, tag)));
			} else if (revision == UNKNOWN) {
				// The local resource is out of sync with the remote.
				// Create a RemoteFile associated with the tag so we are assured of getting the proper revision
				// (Note: this will replace the RemoteFile added from the local base)
				children.put(name, new RemoteFile(remote, 
					d.getSyncState(), 
					name, 
					null, /* the revision will be fetched later */
					getKeywordMode((ICVSFile)children.get(name)), /* get the keyword mode from the local file*/
					tagForRemoteFolder(remote, tag)));
			} else if (revision == DELETED) {
				// This should have been deleted while creating from the local resources.
				// If it wasn't, delete it now.
				if (children.containsKey(name))
					children.remove(name);
			} else {
				// We should never get here
			}
			monitor.worked(1);
		}

		// Add the children to the remote folder tree
		remote.setChildren(children.values().toArray(new ICVSRemoteResource[children.size()]));
		
		// We have to delay building the child folders to support the proper fetching of new directories
		// due to the fact that the same CVS home directory (i.e. the same root directory) must
		// be used for all requests sent over the same connection
		Iterator childIterator = children.entrySet().iterator();
		List<RemoteFolderTree> emptyChildren = new ArrayList<>();
		while (childIterator.hasNext()) {
			Map.Entry entry = (Map.Entry)childIterator.next();
			if (((RemoteResource)entry.getValue()).isFolder()) {
				RemoteFolderTree remoteFolder = (RemoteFolderTree)entry.getValue();
				String name = (String)entry.getKey();
				ICVSFolder localFolder;
				DeltaNode d = (DeltaNode)deltas.get(name);
				// for directories that are new on the server 
				if (d!=null && d.getRevision() == FOLDER)
					localFolder = null;
				else
					localFolder = local.getFolder(name);
				buildRemoteTree(session, localFolder, remoteFolder, Util.appendPath(localPath, name), monitor);
				// Record any children that are empty
				if (isPruneEmptyDirectories() && remoteFolder.getChildren().length == 0) {
					// Prune if the local folder is also empty.
					if (localFolder == null || (localFolder.members(ICVSFolder.ALL_EXISTING_MEMBERS).length == 0))
						emptyChildren.add(remoteFolder);
					else {
						// Also prune if the tag we are fetching is not HEAD and differs from the tag of the local folder
						FolderSyncInfo info = localFolder.getFolderSyncInfo();
						if (tag != null && info != null && ! tag.equals(CVSTag.DEFAULT) && ! tag.equals(info.getTag()))
							emptyChildren.add(remoteFolder);
					}
				}
			}
		}
		
		// Prune any empty child folders
		if (isPruneEmptyDirectories() && !emptyChildren.isEmpty()) {
			List<ICVSRemoteResource> newChildren = new ArrayList<>();
			newChildren.addAll(Arrays.asList(remote.getChildren()));
			newChildren.removeAll(emptyChildren);
			remote.setChildren(newChildren.toArray(new ICVSRemoteResource[newChildren.size()]));

		}
	}
	
	/*
	 * This method fetches the delta between the local state and the remote state of the resource tree
	 * and records the deltas in the fileDeltas instance variable
	 * 
	 * Returns the list of changed files
	 */
	private List<String> fetchDelta(Session session, String[] arguments, final IProgressMonitor monitor) throws CVSException {
		
		// Create an listener that will accumulate new and removed files and folders
		IUpdateMessageListener listener = new IUpdateMessageListener() {
			public void directoryInformation(ICVSFolder root, String path, boolean newDirectory) {
				if (newDirectory) {
					// Record new directory with parent so it can be retrieved when building the parent
					recordDelta(path, FOLDER, Update.STATE_NONE);
					monitor.subTask(NLS.bind(CVSMessages.RemoteFolderTreeBuilder_receivingDelta, new String[] { Util.toTruncatedPath(path, 3) })); 
				}
			}
			public void directoryDoesNotExist(ICVSFolder root, String path) {
				// Record removed directory with parent so it can be removed when building the parent
				if (path.length() == 0) {
					rootDoesNotExist = true;
				} else {
					recordDelta(path, DELETED, Update.STATE_NONE);
					monitor.subTask(NLS.bind(CVSMessages.RemoteFolderTreeBuilder_receivingDelta, new String[] { Util.toTruncatedPath(path, 3) })); 
				}
			}
			public void fileInformation(int type, ICVSFolder root, String filename) {
				// Cases that do not require action are:
				//	case 'A' :  = A locally added file that does not exists remotely
				//	case '?' :  = A local file that has not been added and does not exists remotely
				//  case 'M' :  = A locally modified file that has not been modified remotely
				switch(type) {
					case Update.STATE_MERGEABLE_CONFLICT :
					case Update.STATE_CONFLICT : 
								// We have an remote change to a modified local file
								// The change could be a local change conflicting with a remote deletion.
								// If so, the deltas may already have a DELETED for the file.
								// We shouldn't override this DELETED
								Map deltas = fileDeltas.get(Util.removeLastSegment(filename));
								DeltaNode d = deltas != null ? (DeltaNode)deltas.get(Util.getLastSegment(filename)) : null;
								if ((d!=null) && (d.getRevision() == DELETED))
									break;
					case Update.STATE_DELETED : // We have a locally removed file that still exists remotely
					case Update.STATE_REMOTE_CHANGES : // We have an remote change to an unmodified local file
								changedFiles.add(filename);
								recordDelta(filename, UNKNOWN, type);
								monitor.subTask(NLS.bind(CVSMessages.RemoteFolderTreeBuilder_receivingDelta, new String[] { Util.toTruncatedPath(filename, 3) })); 
								break;
				}	
			}
			public void fileDoesNotExist(ICVSFolder root, String filename) {
				recordDelta(filename, DELETED, Update.STATE_NONE);
				monitor.subTask(NLS.bind(CVSMessages.RemoteFolderTreeBuilder_receivingDelta, new String[] { Util.toTruncatedPath(filename, 3) })); 
			}
		};
		
		// Perform a "cvs -n update -d [-r tag] ." in order to get the
		// messages from the server that will indicate what has changed on the 
		// server.
		IStatus status = Command.SYNCUPDATE.execute(session,
			new GlobalOption[] { Command.DO_NOT_CHANGE },
			updateLocalOptions,
			arguments,
			new UpdateListener(listener),
			monitor);
		if (status.getCode() == CVSStatus.SERVER_ERROR) {
			CVSServerException e = new CVSServerException(status);
			if (e.isNoTagException()) {
				// This error indicates that the complete subtree 
				// being fetched does not have any files for the tag being queried
				rootDoesNotExist = true;
			} else if (e.containsErrors()) {
				// Log the error
				CVSProviderPlugin.log(e);
			}
		}
		return changedFiles;
	}
	/*
	 * Fetch the children of a previously unknown directory.
	 * 
	 * The fetch may do up to 2 units of work in the provided monitor.
	 */
	private void fetchNewDirectory(Session session, RemoteFolderTree newFolder, String localPath, final IProgressMonitor monitor) throws CVSException {
		
		// Create an listener that will accumulate new files and folders
		IUpdateMessageListener listener = new IUpdateMessageListener() {
			public void directoryInformation(ICVSFolder root, String path, boolean newDirectory) {
				if (newDirectory) {
					// Record new directory with parent so it can be retrieved when building the parent
					// NOTE: Check path prefix
					recordDelta(path, FOLDER, Update.STATE_NONE);
					monitor.subTask(NLS.bind(CVSMessages.RemoteFolderTreeBuilder_receivingDelta, new String[] { Util.toTruncatedPath(path, 3) })); 
				}
			}
			public void directoryDoesNotExist(ICVSFolder root, String path) {
			}
			public void fileInformation(int type, ICVSFolder root, String filename) {
				// NOTE: Check path prefix
				changedFiles.add(filename);
				recordDelta(filename, ADDED, type);
				monitor.subTask(NLS.bind(CVSMessages.RemoteFolderTreeBuilder_receivingDelta, new String[] { Util.toTruncatedPath(filename, 3) })); 
			}
			public void fileDoesNotExist(ICVSFolder root, String filename) {
			}
		};

		// NOTE: Should use the path relative to the remoteRoot
		IStatus status = Command.UPDATE.execute(session,
			new GlobalOption[] { Command.DO_NOT_CHANGE },
			updateLocalOptions,
			new String[] { localPath },
			new UpdateListener(listener),
			Policy.subMonitorFor(monitor, 1)); 
		if (status.getCode() == CVSStatus.SERVER_ERROR) {
			CVSServerException e = new CVSServerException(status);
			if ( ! e.isNoTagException() && e.containsErrors())
				throw e;
			// we now know that this is an exception caused by a cvs bug.
			// if the folder has no files in it (just subfolders) cvs does not respond with the subfolders...
			// workaround: retry the request with no tag to get the directory names (if any)
			Policy.checkCanceled(monitor);
			status = Command.UPDATE.execute(session,
				new GlobalOption[] { Command.DO_NOT_CHANGE },
				getOptionsWithoutTag(),
				new String[] { localPath },
				new UpdateListener(listener),
				Policy.subMonitorFor(monitor, 1));
			if (status.getCode() == CVSStatus.SERVER_ERROR) {
				throw new CVSServerException(status);
			}
		}
	}
	
	// Get the file revisions for the given filenames
	private void fetchFileRevisions(Session session, String[] fileNames, final IProgressMonitor monitor) throws CVSException {
		
		// Create a listener for receiving the revision info
		final List<CVSException> exceptions = new ArrayList<>();
		IStatusListener listener = new IStatusListener() {
			public void fileStatus(ICVSFolder root, String path, String remoteRevision) {
				try {
					updateRevision(path, remoteRevision);
					monitor.subTask(NLS.bind(CVSMessages.RemoteFolderTreeBuilder_receivingRevision, new String[] { Util.toTruncatedPath(path, 3) })); 
				} catch (CVSException e) {
					exceptions.add(e);
				}
			}
		};
			
		// Perform a "cvs status..." with a custom message handler
		IStatus status = Command.STATUS.execute(session,
			Command.NO_GLOBAL_OPTIONS,
			Command.NO_LOCAL_OPTIONS,
			fileNames,
			new StatusListener(listener),
			monitor);
		if (status.getCode() == CVSStatus.SERVER_ERROR) {
			throw new CVSServerException(status);
		}
		
		// Report any exceptions that occurred fetching the revisions
		if ( ! exceptions.isEmpty()) {
			if (exceptions.size() == 1) {
				throw exceptions.get(0);
			} else {
				MultiStatus multi = new MultiStatus(CVSProviderPlugin.ID, 0, CVSMessages.RemoteFolder_errorFetchingRevisions, null); 
				for (int i = 0; i < exceptions.size(); i++) {
					multi.merge(exceptions.get(i).getStatus());
				}
				throw new CVSException(multi);
			}
		}
	}

	protected boolean isPruneEmptyDirectories() {
		return false;
	}
	/*
	 * Record the deltas in a double map where the outer key is the parent directory
	 * and the inner key is the file name. The value is the revision of the file or
	 * DELETED (file or folder). New folders have a revision of FOLDER.
	 * 
	 * A revision of UNKNOWN indicates that the revision has not been fetched
	 * from the repository yet.
	 */
	private void recordDelta(String path, String revision, int syncState) {
		if (revision == FOLDER) {
			newFolderExist = true;
		}
		String parent = Util.removeLastSegment(path);
		Map<String, DeltaNode> deltas = fileDeltas.get(parent);
		if (deltas == null) {
			deltas = new HashMap<>();
			fileDeltas.put(parent, deltas);
		}
		String name = Util.getLastSegment(path);
		deltas.put(name, new DeltaNode(name, revision, syncState));
	}
	
	private void updateRevision(String path, String revision) throws CVSException {
		RemoteFolderTree folder = getRecoredRemoteFolder(Util.removeLastSegment(path));
		if (folder == null) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSMessages.RemoteFolderTreeBuilder_missingParent, new String[] { path.toString(), revision }), root);
			throw new CVSException(status);
		}
		((RemoteFile)folder.getFile(Util.getLastSegment(path))).setRevision(revision);
	}
	
	/*
	 * Return the tag that should be associated with a remote folder.
	 * 
	 * This method is used to ensure that new directories contain the tag
	 * derived from the parent local folder when appropriate. For instance,
	 * 
	 * The tag should be the provided tag. However, if tag is null, the 
	 * tag for the folder should be derived from the provided reference folder
	 * which could be the local resource corresponding to the remote or the parent
	 * of the remote.
	 */
	private CVSTag tagForRemoteFolder(ICVSFolder folder, CVSTag tag) throws CVSException {
		return tag == null ? folder.getFolderSyncInfo().getTag() : tag;
	}
	
	private boolean isOrphanedSubtree(ICVSFolder mFolder) throws CVSException {
		return mFolder.isCVSFolder() && ! mFolder.isManaged() && ! mFolder.equals(root) && mFolder.getParent().isCVSFolder();
	}
	
	private void recordRemoteFolder(RemoteFolderTree remote) throws CVSException {
		String path = remote.getFolderSyncInfo().getRemoteLocation();
		remoteFolderTable.put(Util.asPath(path), remote);
	}
	
	private RemoteFolderTree getRecoredRemoteFolder(String path) {
		return remoteFolderTable.get(Util.asPath(path));
	}

	/**
	 * This method returns an array of the files that differ between the local and remote trees.
	 * The files are represented as a String that contains the path to the file in the remote or local trees.
	 * @return an array of differing files
	 */
	public String[] getFileDiffs() {
		return changedFiles.toArray(new String[changedFiles.size()]);
	}
}
