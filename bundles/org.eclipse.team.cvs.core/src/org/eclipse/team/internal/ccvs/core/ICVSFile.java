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
package org.eclipse.team.internal.ccvs.core;

import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.syncinfo.NotifyInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * The CVS analog of a file. CVS files have access to synchronization information
 * that describes their association with the CVS repository. CVS files also provide 
 * mechanisms for sending and receiving content.
 * 
 * @see ICVSResource
 */
public interface ICVSFile extends ICVSResource, ICVSStorage {
	
	// Constants used to indicate the type of updated response from the server
	public static final int UPDATED = 1;
	public static final int MERGED = 2;
	public static final int UPDATE_EXISTING = 3;
	public static final int CREATED = 4;
	
	// Constants used to indicate temporary watches
	public static final int NO_NOTIFICATION = 0;
	public static final int NOTIFY_ON_EDIT = 1;
	public static final int NOTIFY_ON_UNEDIT = 2;
	public static final int NOTIFY_ON_COMMIT = 4;
	public static final int NOTIFY_ON_ALL = NOTIFY_ON_EDIT | NOTIFY_ON_UNEDIT | NOTIFY_ON_COMMIT;
	
	// Constants used to indicate modification state when setting sync info
	public static final int UNKNOWN = 0;
	public static final int CLEAN = 1;
	public static final int DIRTY = 2;

	/**
	 * Answers the workspace synchronization information for this resource. This would 
	 * typically include information from the <b>Entries</b> file that is used to track
	 * the base revisions of local CVS resources.
	 * 
	 * @return the synchronization information for this resource, or <code>null</code>
	 * if the resource does not have synchronization information available.
	 */
	public byte[] getSyncBytes() throws CVSException;

	/**
	 * Called to set the workspace synchronization information for a resource. To
	 * clear sync information call <code>unmanage</code>. The sync info will
	 * become the persisted between workbench sessions.
	 * 
	 * Note: This method makes use of a ResourceSyncInfo object which has the parsed 
	 * contents of the resource sync info. Clients can manipulate the values using
	 * MutableResourceSyncInfo and then set the sync info using this method.
	 * 
	 * @param info the resource synchronization to associate with this resource.
	 */	
	public void setSyncInfo(ResourceSyncInfo info, int modificationState) throws CVSException;
		
	/**
	 * Called to set the workspace synchronization information for a resource. To
	 * clear sync information call <code>unmanage</code>. The sync info will
	 * become the persisted between workbench sessions.
	 * 
	 * Note: This method sets the sync info to the bytes provided as-is. It is the caller's
	 * responsibility to ensure that these bytes are of the proper format. Use with caution.
	 * 
	 * @param info the resource synchronization to associate with this resource.
	 */	
	public void setSyncBytes(byte[] syncBytes, int modificationState) throws CVSException;
	
	/**
	 * Sets the file to read-only (<code>true</code>) or writable (<code>false</code>).
	 * 
	 * This method is used by the command framework and should not be used by other clients.
	 * Other clients should use <code>edit</code> and <code>unedit</code> instead as they
	 * will report the change to the server if appropriate.
	 */
	void setReadOnly(boolean readOnly) throws CVSException;
	
	/**
	 * Answers whether the file is read-only or not. If a file is read-only, <code>edit</code>
	 * should be invoked to make the file editable.
	 */
	boolean isReadOnly() throws CVSException;
	
	/**
	 * Sets the file to be executable (<code>true</code>) or not executable 
	 * (<code>false</code>) if the platform supports it.
	 */
	public void setExecutable(boolean executable) throws CVSException;

	/**
	 * Answers whether the file is executable or not. 
	 * 
	 * @return <code>false</code> if the platform doesn't support the executable flag.
	 */
	public boolean isExecutable() throws CVSException;
	
	/**
	 * Copy the resource to another file in the same directory
	 * 
	 * This method is used by the command framework and should not be used by other clients.
	 */
	void copyTo(String filename) throws CVSException;
	
	/**
	 * Answers the current timestamp for this file with second precision.
	 * 
	 * This method is used by the command framework and should not be used by other clients.
	 */
	Date getTimeStamp();

	/**
	 * If the date is <code>null</code> then the current time is used. After setTimeStamp is
	 * invoked, it is assumed that the file is CLEAN. If this is not the case, it is the clients
	 * responsibility to invoke setSyncBytes() with the appropriate modification state.
	 * 
	 * This method is used by the command framework and should not be used by other clients.
	 */
	void setTimeStamp(Date date) throws CVSException;
	
	/**
	 * Answers <code>true</code> if the file has changed since it was last updated
	 * from the repository, if the file does not exist, or is not managed. And <code>false</code> 
	 * if it has not changed.
	 */
	boolean isModified(IProgressMonitor monitor) throws CVSException;
	
	/**
	 * Answers the revision history for this file. This is similar to the
	 * output of the log command.
	 */
	public ILogEntry[] getLogEntries(IProgressMonitor monitor) throws TeamException;
	
	/**
	 * Mark the file as checked out to allow local editing (analogous to "cvs edit"). 
	 * If this method is invoked when <code>isCheckedOut()</code> returns <code>false</code>, 
	 * a notification message that will be sent to the server on the next connection
	 * If <code>isCheckedOut()</code> returns <code>true</code> then nothing is done.
	 * 
	 * @param notifications the set of operations for which the local user would like notification
	 * while the local file is being edited.
	 * @param notifyForWritable 
	 */
	public void edit(int notifications, boolean notifyForWritable, IProgressMonitor monitor) throws CVSException;

	/**
	 * Undo a checkout of the file (analogous to "cvs unedit").
	 * If this method is invoked when <code>isCheckedOut()</code> returns <code>true</code>, 
	 * a notification message that will be sent to the server on the next connection
	 * If <code>isCheckedOut()</code> returns <code>false</code> then nothing is done.
	 */
	public void unedit(IProgressMonitor monitor) throws CVSException;

	/**
	 * This method is invoked by the checked-in handler after the file
	 * has been committed.
	 * @param entryLine the entry line recieved from the server (can be null)
	 * @param commit whether the checkin is comming from a cvs commit or not
	 */
	public void checkedIn(String entryLine, boolean commit) throws CVSException;
		
	/**
	 * Answer any pending notification information associated with the receiver.
	 * 
	 * This method is used by the command framework and should not be used by other clients.
	 */
	public NotifyInfo getPendingNotification() throws CVSException;
	
	/**
	 * Indicate to the file that the pending notification was successfully communicated to the server.
	 * 
	 * This method is used by the command framework and should not be used by other clients.
	 */
	public void notificationCompleted() throws CVSException;
	
	/**
	 * Indicate whether the file has been "cvs edit"ed. This is determined by
	 * looking in the CVS/Base folder for a file of the same name as the
	 * file (i.e. no files are read so the method can be called by time critical
	 * code like menu enablement).
	 * 
	 * @return boolean
	 */
	public boolean isEdited() throws CVSException;

}
