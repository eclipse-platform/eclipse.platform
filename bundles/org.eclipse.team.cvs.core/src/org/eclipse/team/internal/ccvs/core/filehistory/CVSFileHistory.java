/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

package org.eclipse.team.internal.ccvs.core.filehistory;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;

public class CVSFileHistory extends FileHistory {

	public static final int REFRESH_LOCAL = 1;
	public static final int REFRESH_REMOTE = 2;
	
	public static final int REFRESH_ALL = REFRESH_LOCAL | REFRESH_REMOTE;
	
	private final ICVSFile cvsFile;
	//used to hold all revisions (changes based on filtering)
	protected IFileRevision[] revisions;
	//used to hold all of the remote revisions
	protected IFileRevision[] remoteRevisions;
	//used to hold all of the local revisions
	protected IFileRevision[] localRevisions;
	protected boolean includeLocalRevisions;
	protected boolean includeRemoteRevisions;
	protected boolean includesExists;
	protected boolean refetchRevisions;

	private int flag;

	/*
	 * Creates a new CVSFile history that will fetch remote revisions by default.
	 */
	public CVSFileHistory(ICVSFile file) {
		Assert.isNotNull(file);
		this.cvsFile = file;
		this.includeLocalRevisions = false;
		this.includeRemoteRevisions = true;
		this.refetchRevisions = true;
		this.flag = 0;
	}

	/*
	 * 
	 * Creates a new CVSFile history that will fetch remote revisions by default.
	 * The flag passed can be IFileHistoryProvider.SINGLE_REVISION or IFileHistoryProvider.SINGLE_LINE_OF_DESCENT
	 */
	public CVSFileHistory(ICVSFile file, int flag) {
		Assert.isNotNull(file);
		this.cvsFile = file;
		this.includeLocalRevisions = false;
		this.includeRemoteRevisions = true;
		this.refetchRevisions = true;
		this.flag = flag;
	}

	public IFileRevision[] getFileRevisions() {
		if (revisions == null)
			return new IFileRevision[0];
		return revisions;
	}

	/**
	 * Refreshes the revisions for this CVS file. It may or may not contact the server to get new revisions.
	 * 
	 * @param monitor	a progress monitor
	 */
	public void refresh(int flags, IProgressMonitor monitor) throws TeamException {
		if (flags == CVSFileHistory.REFRESH_LOCAL) {
			fetchLocalOnly(monitor);
			return;
		}
			
		if (refetchRevisions) {
			monitor.beginTask(NLS.bind(CVSMessages.CVSFileHistory_0, cvsFile.getRepositoryRelativePath()), 300);
			try {
				ILogEntry[] entries = cvsFile
						.getLogEntries(SubMonitor.convert(monitor, 200));
				
				if (entries.length == 0){
					//Get the parent folder
					ICVSFolder folder = cvsFile.getParent();
					if (folder.isManaged()){
						String remoteFolderLocation = folder.getRemoteLocation(folder);
						if (remoteFolderLocation != null) {
							String remoteFileName = remoteFolderLocation.concat(Session.SERVER_SEPARATOR + cvsFile.getName());
							//Create remote file
							CVSTeamProvider pro = (CVSTeamProvider) RepositoryProvider.getProvider(cvsFile.getIResource().getProject());
							if (pro != null){
								CVSWorkspaceRoot root = pro.getCVSWorkspaceRoot();
								CVSRepositoryLocation location = CVSRepositoryLocation.fromString(root.getRemoteLocation().getLocation(false));
								RemoteFile remFile = RemoteFile.create(remoteFileName, location);
								entries=remFile.getLogEntries(monitor);
							}
						}
					}
				}
				
				if (flag == IFileHistoryProvider.SINGLE_REVISION) {
					String revisionNumber = cvsFile.getSyncInfo().getRevision();
					for (ILogEntry entry : entries) {
						if (entry.getRevision().equals(revisionNumber)) {
							remoteRevisions = new IFileRevision[]{new CVSFileRevision(entry)};
							revisions = new IFileRevision[1];
							//copy over remote revisions
							System.arraycopy(remoteRevisions, 0, revisions, 0, remoteRevisions.length);
							break;
						}
					}

				} else if (flag == IFileHistoryProvider.SINGLE_LINE_OF_DESCENT) {
					CVSTag tempTag = cvsFile.getSyncInfo().getTag();
					ArrayList<ILogEntry> entriesOfInterest = new ArrayList<>();
					for (ILogEntry entry : entries) {
						CVSTag[] tags = entry.getTags();
						for (CVSTag tag : tags) {
							if (tag.getType() == tempTag.getType()) {
								if (tempTag.getType() == CVSTag.BRANCH && tempTag.getName().equals(tag.getName())) {
									entriesOfInterest.add(entry);
									break;
								} else {
									entriesOfInterest.add(entry);
									break;
								}
							}
						}
					}

					//always fetch the remote revisions, just filter them out from the returned array
					remoteRevisions = new IFileRevision[entriesOfInterest.size()];
					Iterator iter = entriesOfInterest.iterator();
					int i = 0;
					while (iter.hasNext()) {
						remoteRevisions[i++] = new CVSFileRevision((ILogEntry) iter.next());
					}

					//copy over remote revisions
					revisions = new IFileRevision[remoteRevisions.length];
					System.arraycopy(remoteRevisions, 0, revisions, 0, remoteRevisions.length);

				} else {
					localRevisions = new IFileRevision[0];
					//always fetch the local revisions, just filter them out from the returned array if not wanted
					IResource localResource = cvsFile.getIResource();
					includesExists = false;
					if (localResource != null && localResource instanceof IFile) {
						//get the local revisions
						IFileState[] localHistoryState;
						try {
							localHistoryState = ((IFile) localResource).getHistory(SubMonitor.convert(monitor, 100));
							localRevisions = convertToFileRevision(localHistoryState, SubMonitor.convert(monitor, 100));
							includesExists = (localRevisions.length > 0);
						} catch (CoreException e) {
							TeamException.asTeamException(e);
						}
					}

					//always fetch the remote revisions, just filter them out from the returned array
					remoteRevisions = new IFileRevision[entries.length];
					for (int i = 0; i < entries.length; i++) {
						remoteRevisions[i] = new CVSFileRevision(entries[i]);
					}

					revisions = new IFileRevision[0];
					arrangeRevisions();
				}
			}  finally {
				monitor.done();
			}
		} else {
			//don't refetch revisions just return revisions with local revisions as requested
			arrangeRevisions();
		}
		
	}

	private void arrangeRevisions() {
		if (revisions != null) {
			if (includeLocalRevisions && includeRemoteRevisions) {
				//Local + Remote mode
				revisions = new IFileRevision[remoteRevisions.length + localRevisions.length];
				//copy over remote revisions
				System.arraycopy(remoteRevisions, 0, revisions, 0, remoteRevisions.length);
				//copy over local revisions
				System.arraycopy(localRevisions, 0, revisions, remoteRevisions.length, localRevisions.length);
			} else if (includeLocalRevisions) {
				//Local mode only
				revisions = new IFileRevision[localRevisions.length];
				//copy over local revisions
				System.arraycopy(localRevisions, 0, revisions, 0, localRevisions.length);
			} else if (includeRemoteRevisions) {
				//Remote mode and fall through for Local + Remote mode where no Locals exist
				revisions = new IFileRevision[remoteRevisions.length];
				//copy over remote revisions
				System.arraycopy(remoteRevisions, 0, revisions, 0, remoteRevisions.length);
			}
		}
	}

	public IFileRevision getFileRevision(String id) {
		IFileRevision[] revisions = getFileRevisions();
		for (IFileRevision revision : revisions) {
			if (revision.getContentIdentifier().equals(id)) {
				return revision;
			}
		}
		return null;
	}

	public IFileRevision[] getContributors(IFileRevision revision) {

		IFileRevision[] revisions = getFileRevisions();

		//the predecessor is the file with a timestamp that is the largest timestamp
		//from the set of all timestamps smaller than the root file's timestamp
		IFileRevision fileRevision = null;
		for (IFileRevision r : revisions) {
			if (((CVSFileRevision) r).isPredecessorOf(revision)) {
				//no revision has been set as of yet
				if (fileRevision == null) {
					fileRevision = r;
				}
				//this revision is a predecessor - now check to see if it comes
				//after the current predecessor, if it does make it the current predecessor
				if (r.getTimestamp() > fileRevision.getTimestamp()) {
					fileRevision = r;
				}
			}
		}
		if (fileRevision == null)
			return new IFileRevision[0];
		return new IFileRevision[] {fileRevision};
	}

	public IFileRevision[] getTargets(IFileRevision revision) {
		IFileRevision[] revisions = getFileRevisions();

		//the predecessor is the file with a timestamp that is the largest timestamp
		//from the set of all timestamps smaller than the root file's timestamp
		ArrayList<IFileRevision> directDescendents = new ArrayList<>();

		for (IFileRevision r : revisions) {
			if (((CVSFileRevision) r).isDescendentOf(revision)) {
				directDescendents.add(r);
			}
		}
		return directDescendents.toArray(new IFileRevision[directDescendents.size()]);
	}

	private IFileRevision[] convertToFileRevision(IFileState[] localRevisions, IProgressMonitor monitor) {
		boolean modified = false;
		try {
			modified = cvsFile.isModified(monitor);
		} catch (CVSException e) {
		}

		IFile localFile = (IFile) cvsFile.getIResource();
		boolean localFileExists = (localFile != null && localFile.exists());
		int arrayLength = 0;
		if (modified && localFileExists)
			arrayLength++;

		arrayLength += localRevisions.length;

		IFileRevision[] fileRevisions = new IFileRevision[arrayLength];
		for (int i = 0; i < localRevisions.length; i++) {
			IFileState localFileState = localRevisions[i];
			CVSLocalFileRevision localRevision = new CVSLocalFileRevision(localFileState);
			fileRevisions[i] = localRevision;
		}

		if (modified && localFileExists) {
			CVSLocalFileRevision currentFile = new CVSLocalFileRevision(localFile);
			CVSFileHistoryProvider provider = new CVSFileHistoryProvider();
			currentFile.setBaseRevision(provider.getWorkspaceFileRevision(localFile));
			fileRevisions[localRevisions.length] = currentFile;
		}

		return fileRevisions;
	}

	public void includeLocalRevisions(boolean flag) {
		this.includeLocalRevisions = flag;
	}

	public boolean getIncludesExists() {
		return includesExists;
	}

	public void setRefetchRevisions(boolean refetch) {
		this.refetchRevisions = refetch;
	}

	public void includeRemoteRevisions(boolean flag) {
		this.includeRemoteRevisions = flag;
	}

	public void fetchLocalOnly(IProgressMonitor monitor) {
		try{
		localRevisions = new IFileRevision[0];
		//always fetch the local revisions, just filter them out from the returned array if not wanted
		IResource localResource = cvsFile.getIResource();
		includesExists = false;
		if (localResource != null && localResource instanceof IFile) {
			//get the local revisions
			IFileState[] localHistoryState = ((IFile) localResource).getHistory(SubMonitor.convert(monitor, 100));
			localRevisions = convertToFileRevision(localHistoryState, SubMonitor.convert(monitor, 100));
			includesExists = (localRevisions.length > 0);
		}

		if (remoteRevisions == null)
			remoteRevisions = new IFileRevision[0];
		revisions = new IFileRevision[0];
		arrangeRevisions();
		} catch (CoreException ex){
			//nothing to do - calling getFileRevisions() when revisions is null will result in
			//revisions returning an empty array
		} finally {
			monitor.done();
		}
	}

	public boolean isInitialized() {
		return revisions != null;
	}

	public boolean isIncludeLocal() {
		return includeLocalRevisions;
	}		
}
