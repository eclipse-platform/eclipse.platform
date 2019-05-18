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
 *     Red Hat Incorporated - is/setExecutable() code
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.resources;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.variants.CachedResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.*;
import org.eclipse.team.internal.ccvs.core.client.Log;
import org.eclipse.team.internal.ccvs.core.client.listeners.ILogEntryListener;
import org.eclipse.team.internal.ccvs.core.client.listeners.LogListener;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.filehistory.CVSResourceVariantFileRevision;
import org.eclipse.team.internal.ccvs.core.filesystem.CVSURI;
import org.eclipse.team.internal.ccvs.core.syncinfo.*;

/**
 * This class provides the implementation of ICVSRemoteFile and IManagedFile for
 * use by the repository and sync view.
 */
public class RemoteFile extends RemoteResource implements ICVSRemoteFile  {
	/*
	 * Listener for accumulating the entries fetched using the "cvs log" command
	 */
	private final class LogEntryListener implements ILogEntryListener {
		private final List<ILogEntry> entries = new ArrayList<>();
		@Override
		public void handleLogEntryReceived(ILogEntry entry) {
			if (entry.getRemoteFile().getRepositoryRelativePath().equals(getRepositoryRelativePath())) {
				entries.add(entry);
			}
		}
		public ILogEntry[] getEntries() {
			return entries.toArray(new ILogEntry[entries.size()]);
		}
	}

	// sync info in byte form
	private byte[] syncBytes;
	// cache the log entry for the remote file
	private ILogEntry entry;
	// state that indicates that the handle is actively fetching content
	private boolean fetching = false;
	// executable bit
	private boolean executable = false;
			
	/**
	 * Static method which creates a file as a single child of its parent.
	 * This should only be used when one is only interested in the file alone.
	 * 
	 * The returned RemoteFile represents the base of the local resource.
	 * If the local resource does not have a base, then null is returned
	 * even if the resource does exists remotely (e.g. created by another party).
	 */
	public static RemoteFile getBase(RemoteFolder parent, ICVSFile managed) throws CVSException {
		Assert.isNotNull(parent, "A parent folder must be provided for file " + managed.getName()); //$NON-NLS-1$
		byte[] syncBytes = managed.getSyncBytes();
		if ((syncBytes == null) || ResourceSyncInfo.isAddition(syncBytes)) {
			// Either the file is unmanaged or has just been added (i.e. doesn't necessarily have a remote)
			return null;
		}
		if (ResourceSyncInfo.isDeletion(syncBytes)) {
			syncBytes = ResourceSyncInfo.convertFromDeletion(syncBytes);
		}
		RemoteFile file = new RemoteFile(parent, syncBytes);
		parent.setChildren(new ICVSRemoteResource[] {file});
		return file;
	}
	
	/**
	 * This method is used by the CVS subscribers to create file handles.
	 */
	public static RemoteFile fromBytes(IResource local, byte[] bytes, byte[] parentBytes) throws CVSException {
		Assert.isNotNull(bytes);
		Assert.isTrue(local.getType() == IResource.FILE);
		RemoteFolder parent = RemoteFolder.fromBytes(local.getParent(), parentBytes);
		RemoteFile file = new RemoteFile(parent, bytes);
		parent.setChildren(new ICVSRemoteResource[] {file});
		return file;
	}
	
	/**
	 * Create a remote file handle for the given file path that is relative to the
	 * given location.
	 */
	public static RemoteFile create(String filePath, ICVSRepositoryLocation location) {
		return create(filePath, location, null, null);
	}
	
	/**
	 * Create a remote file handle for the given file path that is relative to the
	 * given location.
	 */
	public static RemoteFile create(String filePath, ICVSRepositoryLocation location, CVSTag tag, String revision) {
		Assert.isNotNull(filePath);
		Assert.isNotNull(location);
		IPath path = new Path(null, filePath);
		if (tag != null && revision != null && tag.getName().equals(revision))
			tag = null;
		RemoteFolder parent = new RemoteFolder(null /* parent */, location, path.removeLastSegments(1).toString(), tag /* tag */);
		RemoteFile file = new RemoteFile(parent, Update.STATE_NONE, path.lastSegment(), revision /* revision */, null /* keyword mode */, tag /* tag */);
		parent.setChildren(new ICVSRemoteResource[] {file});
		return file;
	}
	
	/**
	 * Constructor for RemoteFile that should be used when nothing is know about the
	 * file ahead of time.
	 * @param parent the folder that is the parent of the file
	 * @param workspaceSyncState the workspace state (use Update.STATE_NONE if unknown)
	 * @param name the name of the file
	 * @param revision revision of the file or <code>null</code> if the revision is not known
	 * @param keywordMode keyword mode of the file or <code>null</code> if the mode is not known
	 * @param tag tag for the file
	 */
	public RemoteFile(RemoteFolder parent, int workspaceSyncState, String name, String revision, KSubstOption keywordMode, CVSTag tag) {
		this(parent, name, workspaceSyncState, getSyncBytes(name, revision, keywordMode, tag));
	}
	
	private static byte[] getSyncBytes(String name, String revision, KSubstOption keywordMode, CVSTag tag) {
		if (revision == null) {
			revision = ResourceSyncInfo.ADDED_REVISION;
		}
		if (keywordMode == null) {
			// Always use a blank mode for remote files so that 
			// the proper mode will be obtained when the contents
			// are fetched
			keywordMode = KSubstOption.fromMode(""); //$NON-NLS-1$
		}
		MutableResourceSyncInfo newInfo = new MutableResourceSyncInfo(name, revision);		
		newInfo.setKeywordMode(keywordMode);
		newInfo.setTag(tag);
		return newInfo.getBytes();
	}
	
	public RemoteFile(RemoteFolder parent, byte[] syncBytes) throws CVSException {
		this(parent, Update.STATE_NONE, syncBytes);
	}
	
	/* package */ RemoteFile(RemoteFolder parent, int workspaceSyncState, byte[] syncBytes) throws CVSException {
		this(parent, ResourceSyncInfo.getName(syncBytes), workspaceSyncState, syncBytes);
	}

	private RemoteFile(RemoteFolder parent, String name, int workspaceSyncState, byte[] syncBytes) {
		super(parent, name);
		this.syncBytes = syncBytes;
		setWorkspaceSyncState(workspaceSyncState);
	}

	@Override
	public void accept(ICVSResourceVisitor visitor) throws CVSException {
		visitor.visitFile(this);
	}

	@Override
	public void accept(ICVSResourceVisitor visitor, boolean recurse) throws CVSException {
		visitor.visitFile(this);
	}
	
	@Override
	public InputStream getContents(IProgressMonitor monitor) throws CVSException {
		try {
			return getStorage(monitor).getContents();
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	@Override
	protected void fetchContents(IProgressMonitor monitor) throws TeamException {
		try {
			aboutToReceiveContents(getSyncBytes());
			internalFetchContents(monitor);
			// If the fetch succeeded but no contents were cached from the server
			// than we can assume that the remote file has no contents.
			if (!isContentsCached()) {
				setContents(new ByteArrayInputStream(new byte[0]), monitor);
			}
		} finally {
			doneReceivingContents();
		}
	}
	
	private void internalFetchContents(IProgressMonitor monitor) throws CVSException {
		monitor.beginTask(CVSMessages.RemoteFile_getContents, 100);
		monitor.subTask(CVSMessages.RemoteFile_getContents);
		if (getRevision().equals(ResourceSyncInfo.ADDED_REVISION)) {
			// The revision of the remote file is not known so we need to use the tag to get the status of the file
			CVSTag tag = getSyncInfo().getTag();
			if (tag == null) tag = CVSTag.DEFAULT;
			RemoteFolderMemberFetcher fetcher = new RemoteFolderMemberFetcher((RemoteFolder)getParent(), tag);
			fetcher.updateFileRevisions(new ICVSFile[] { this }, Policy.subMonitorFor(monitor, 10));
		}
		Session session = new Session(getRepository(), parent, false /* create backups */);
		session.open(Policy.subMonitorFor(monitor, 10), false /* read-only */);
		try {
			IStatus status = Command.UPDATE.execute(
				session,
				Command.NO_GLOBAL_OPTIONS,
				new LocalOption[] { 
					Update.makeTagOption(new CVSTag(getRevision(), CVSTag.VERSION)),
					Update.IGNORE_LOCAL_CHANGES },
				new ICVSResource[] { this },
				null,
				Policy.subMonitorFor(monitor, 80));
			if (status.getCode() == CVSStatus.SERVER_ERROR) {
				throw new CVSServerException(status);
			}
		} finally {
			session.close();
			monitor.done();
		}
	}

	@Override
	public ILogEntry getLogEntry(IProgressMonitor monitor) throws CVSException {
		if (entry == null) {
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(CVSMessages.RemoteFile_getLogEntries, 100); 
			Session session = new Session(getRepository(), parent, false /* output to console */);
			session.open(Policy.subMonitorFor(monitor, 10), false /* read-only */);
			try {
				try {
					LogEntryListener listener = new LogEntryListener();
					IStatus status = Command.LOG.execute(
						session,
						Command.NO_GLOBAL_OPTIONS,
						new LocalOption[] { 
							Log.makeRevisionOption(getRevision())},
						new ICVSResource[] { RemoteFile.this },
						new LogListener(RemoteFile.this, listener),
						Policy.subMonitorFor(monitor, 90));
					ILogEntry[] entries = listener.getEntries();
					if (entries.length == 1) {
						entry = entries[0];
					}
					if (status.getCode() == CVSStatus.SERVER_ERROR) {
						throw new CVSServerException(status);
					}
				} finally {
					monitor.done();
				}
			} finally {
				session.close();
			}
		}
		return entry;
	}
	
	@Override
	public ILogEntry[] getLogEntries(IProgressMonitor monitor) throws CVSException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(CVSMessages.RemoteFile_getLogEntries, 100); 
		Session session = new Session(getRepository(), parent, false /* output to console */);
		session.open(Policy.subMonitorFor(monitor, 10), false /* read-only */);
		try {
			QuietOption quietness = CVSProviderPlugin.getPlugin().getQuietness();
			try {
				CVSProviderPlugin.getPlugin().setQuietness(Command.VERBOSE);
				LogEntryListener listener = new LogEntryListener();  
				IStatus status = Command.LOG.execute(
					session,
					Command.NO_GLOBAL_OPTIONS, Command.NO_LOCAL_OPTIONS,
					new ICVSResource[] { RemoteFile.this }, new LogListener(RemoteFile.this, listener),
					Policy.subMonitorFor(monitor, 90));
				if (status.getCode() == CVSStatus.SERVER_ERROR) {
					throw new CVSServerException(status);
				}
				return listener.getEntries();
			} finally {
				CVSProviderPlugin.getPlugin().setQuietness(quietness);
				monitor.done();
			}
		} finally { 
			session.close();
		}
	}
	
	@Override
	public String getRevision() {
		try {
			return ResourceSyncInfo.getRevision(syncBytes);
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
			return ResourceSyncInfo.ADDED_REVISION;
		}
	}
	
	private KSubstOption getKeywordMode() {
		try {
			return ResourceSyncInfo.getKeywordMode(syncBytes);
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
			return KSubstOption.getDefaultTextMode();
		}
	}
	
	/*
	 * Get a different revision of the remote file.
	 * 
	 * We must also create a new parent since the child is accessed through the parent from within CVS commands.
	 * Therefore, we need a new parent so that we can fetch the contents of the remote file revision
	 */
	public RemoteFile toRevision(String revision) {
		RemoteFolder newParent = new RemoteFolder(null, parent.getRepository(), parent.getRepositoryRelativePath(), parent.getTag());
		RemoteFile file = new RemoteFile(newParent, getWorkspaceSyncState(), getName(), revision, getKeywordMode(), CVSTag.DEFAULT);
		newParent.setChildren(new ICVSRemoteResource[] {file});
		return file;
	}

	@Override
	public ResourceSyncInfo getSyncInfo() {
		try {
			return new ResourceSyncInfo(syncBytes);
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
			return null;
		}
	}
	
	@Override
	public String getRemoteLocation(ICVSFolder stopSearching) throws CVSException {
		return parent.getRemoteLocation(stopSearching) + Session.SERVER_SEPARATOR + getName();
	}
	
	/**
	 * Get the remote path for the receiver relative to the repository location path
	 */
	@Override
	public String getRepositoryRelativePath() {
		String parentPath = parent.getRepositoryRelativePath();
		return parentPath + Session.SERVER_SEPARATOR + getName();
	}
	
	/**
	 * Return the server root directory for the repository
	 */
	@Override
	public ICVSRepositoryLocation getRepository() {
		return parent.getRepository();
	}
	
	@Override
	public void setSyncInfo(ResourceSyncInfo fileInfo, int modificationState) {
		setSyncBytes(fileInfo.getBytes(),modificationState);
	}

	/**
	 * Set the revision for this remote file.
	 * 
	 * @param revision to associated with this remote file
	 */
	public void setRevision(String revision) throws CVSException {
		syncBytes = ResourceSyncInfo.setRevision(syncBytes, revision);
	}		
	
	@Override
	public InputStream getContents() throws CVSException {
		if (!fetching) {
			// Return the cached contents
			if (isContentsCached()) {
				try {
					InputStream cached = getCachedContents();
					if (cached != null) {
						return cached;
					}
				} catch (TeamException e) {
					throw CVSException.wrapException(e);
				}
			}
		}
		// There was nothing cached so return an empty stream.
		// This is done to allow the contents to be fetched
		// (i.e. update sends empty contents and real contents are sent back)
		return new ByteArrayInputStream(new byte[0]);
	}

	@Override
	protected InputStream getCachedContents() throws TeamException {
		if (isHandleCached()) {
			RemoteFile file = (RemoteFile)getCachedHandle();
			if (file != null) {
				byte[] newSyncBytes = file.getSyncBytes();
				if (newSyncBytes != null) {
					// Make sure the sync bytes match the content that is being accessed
					syncBytes = newSyncBytes;
				}
			}
		}
		return super.getCachedContents();
	}
	
	@Override
	public void setContents(InputStream stream, int responseType, boolean keepLocalHistory, IProgressMonitor monitor) throws CVSException {
		try {
			setContents(stream, monitor);
		} catch (TeamException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	@Override
	public void setReadOnly(boolean readOnly) {
		// RemoteFiles are always read only
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}
	
	@Override
	public Date getTimeStamp() {
		return getSyncInfo().getTimeStamp();
	}

	@Override
	public void setTimeStamp(Date date) {
		// RemoteFiles are not mutable so do not support timestamp changes
	}

	@Override
	public void copyTo(String mFile) {		
		// Do nothing
	}
	
	@Override
	public ICVSRemoteResource[] members(IProgressMonitor progress) {
		return new ICVSRemoteResource[0];
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	@Override
	public boolean isFolder() {
		return false;
	}
	
	/*
	 * @see ICVSResource#tag(CVSTag, LocalOption[], IProgressMonitor)
	 * 
	 * The revision of the remote file is used as the base for the tagging operation
	 */
	@Override
	public IStatus tag(final CVSTag tag, final LocalOption[] localOptions, IProgressMonitor monitor) throws CVSException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		Session session = new Session(getRepository(), getParent(), true /* output to console */);
		session.open(Policy.subMonitorFor(monitor, 10), true /* open for modification */);
		try {
			return Command.RTAG.execute(
				session,
				Command.NO_GLOBAL_OPTIONS,
				localOptions,
				new CVSTag(getRevision(), CVSTag.VERSION),
				tag,
				new ICVSRemoteResource[] { RemoteFile.this },
			Policy.subMonitorFor(monitor, 90));
		} finally {
			session.close();
		}
	}
	
	@Override
	public boolean equals(Object target) {
		if (this == target)
			return true;
		if (!(target instanceof RemoteFile))
			return false;
		RemoteFile remote = (RemoteFile) target;
		return super.equals(target) && remote.getRevision().equals(getRevision());
	}

	@Override
	public void edit(int notifications, boolean notifyForWritable, IProgressMonitor monitor) {
		// do nothing
	}

	@Override
	public void unedit(IProgressMonitor monitor) {
		// do nothing
	}

	@Override
	public void notificationCompleted() {
		// do nothing
	}

	@Override
	public NotifyInfo getPendingNotification() {
		return null;
	}

	@Override
	public ICVSRemoteResource forTag(ICVSRemoteFolder parent, CVSTag tag) {
		return new RemoteFile((RemoteFolder)parent, getWorkspaceSyncState(), getName(), getRevision(), getKeywordMode(), tag);
	}

	@Override
	public ICVSRemoteResource forTag(CVSTag tag) {
		RemoteFolderTree remoteFolder = new RemoteFolderTree(null, getRepository(), 
			((ICVSRemoteFolder)getParent()).getRepositoryRelativePath(), 
			tag);
		RemoteFile remoteFile = (RemoteFile)forTag(remoteFolder, tag);
		remoteFolder.setChildren(new ICVSRemoteResource[] { remoteFile });
		return remoteFile;
	}
	@Override
	public void checkedIn(String info, boolean commit) {
		// do nothing
	}
	@Override
	public boolean isEdited() {
		return false;
	}
	@Override
	public byte[] getSyncBytes() {
		return syncBytes;
	}
	@Override
	public void setSyncBytes(byte[] syncBytes, int modificationState) {
		if (fetching) {
			RemoteFile file = (RemoteFile)getCachedHandle();
			if (file == null) {
				cacheHandle();
			} else if (file != this) {
				file.setSyncBytes(syncBytes, modificationState);
			}
		}
		this.syncBytes = syncBytes;
	}

	@Override
	public String toString() {
		return super.toString() + " " + getRevision(); //$NON-NLS-1$
	}

	@Override
	public String getContentIdentifier() {
		return getRevision();
	}

	/**
	 * Callback which indicates that the remote file is about to receive contents that should be cached
	 * @param entryLine
	 */
	public void aboutToReceiveContents(byte[] entryLine) {
		try {
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=189025
			entryLine = ResourceSyncInfo.setSlot(syncBytes, 3, new byte[0]);
		} catch (CVSException e) {
			// log it and proceed
			CVSProviderPlugin.log(e);
		}
		setSyncBytes(entryLine, ICVSFile.CLEAN);
		fetching = true;
	}

	/**
	 * The contents for the file have already been provided.
	 */
	public void doneReceivingContents() {
		fetching = false;
	}
	
	@Override
	public boolean isContentsCached() {
		// Made public for use by FileContentCachingService
		return super.isContentsCached();
	}

	/**
	 * Cache the contents of the given IFile as the contents for this remote file handle.
	 * The caller must ensure that the local file is mapped to the same revision and is
	 * not modified since it was loaded from CVS.
	 * @param file
	 * @throws CoreException
	 * @throws TeamException
	 */
	public void setContents(IFile file, IProgressMonitor monitor) throws TeamException, CoreException {
		setContents(file.getContents(), monitor);
	}

	@Override
	public void setExecutable(boolean executable) throws CVSException {
		// store executable bit;
		this.executable = executable;
		if (!isHandleCached()) {
			cacheHandle();
		}
		RemoteFile file = (RemoteFile)getCachedHandle();
		if (file != this) {
			file.setExecutable(executable);
		}
	}

	@Override
	public boolean isExecutable() throws CVSException {
		// return executable bit
		return executable;
	}
	
	@Override
	public CachedResourceVariant getCachedHandle() {
		return super.getCachedHandle();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IFileRevision.class)
			return (T) new CVSResourceVariantFileRevision(this);
		return super.getAdapter(adapter);
	}
	
	public CVSURI toCVSURI() {
		ResourceSyncInfo info = getSyncInfo();
		return new CVSURI(getRepository(), new Path(getRepositoryRelativePath()), info.getTag(), info.getRevision());
	}
}
