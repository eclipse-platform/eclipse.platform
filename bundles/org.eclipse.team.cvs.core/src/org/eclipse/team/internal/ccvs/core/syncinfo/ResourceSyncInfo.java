/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.syncinfo;

 
import java.text.ParseException;
import java.util.Date;

import org.eclipse.core.runtime.Assert;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
import org.eclipse.team.internal.ccvs.core.resources.CVSEntryLineTag;
import org.eclipse.team.internal.ccvs.core.util.CVSDateFormatter;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * Value (immutable) object that represents workspace state information about a resource contained in
 * a CVS repository. It is a specialized representation of a line in the CVS/Entry file with the addition of 
 * file permissions.
 * <p>
 * ResourceSyncInfo instances are created from entry lines from the CVS server and from the CVS/Entries
 * file. Although both entry lines have slightly different formats (e.g. timestamps) they can safely be passed
 * to the constructor.</p>
 * <p>
 * A class named <code>MutableResourceSyncInfo</code> can be used to modify an existing resource
 * sync or create sync info without an entry line.</p>
 * 
 * Example entry line from the CVS/Entry file:
 * 
 * /new.java/1.2/Fri Dec  7 00:17:52 2001/-kb/
 * D/src////
 *  
 * @see MutableResourceSyncInfo
 * @see ICVSResource#getSyncInfos()
 */
public class ResourceSyncInfo {
		
	// [Note: permissions aren't honoured in this current implementation]
	// safe default permissions. Permissions are saved separately so that the correct permissions
	// can be sent back to the server on systems that don't save execute bits (e.g. windows).
	private static final String DEFAULT_PERMISSIONS = "u=rw,g=rw,o=r"; //$NON-NLS-1$
	private static final String DEFAULT_EXECUTABLE_PERMISSIONS = "u=rwx,g=rwx,o=rx"; //$NON-NLS-1$
	
	// file sync information can be associated with a local resource that has been deleted. This is
	// noted by prefixing the revision with this character.
	private static final String DELETED_PREFIX = "-"; //$NON-NLS-1$
	private static final byte DELETED_PREFIX_BYTE = '-';

	// revision can be locked in repository using "cvs admin -l<rev>" command
	// entry looks like [M revision 1.2.2.3	locked by: igorf;]
	public static final String LOCKEDBY_REGEX = "\\slocked by.+$"; //$NON-NLS-1$

	// a sync element with a revision of '0' is considered a new file that has
	// not been committed to the repo. Is visible so that clients can create sync infos
	// for new files.
	public static final String ADDED_REVISION = "0"; //$NON-NLS-1$
	
	// Timestamp constants used to identify special cases
	protected static final int TYPE_REGULAR = 1;
	protected static final int TYPE_MERGED = 2;
	protected static final int TYPE_MERGED_WITH_CONFLICTS = 3;
	protected static final int TYPE_DELETED_AND_RESTORED = 4;
	
	protected static final String TIMESTAMP_DUMMY = "dummy timestamp"; //$NON-NLS-1$
	protected static final String TIMESTAMP_MERGED = "Result of merge"; //$NON-NLS-1$
	protected static final String TIMESTAMP_MERGED_WITH_CONFLICT = TIMESTAMP_MERGED + "+"; //$NON-NLS-1$
	protected static final String TIMESTAMP_DELETED_AND_RESTORED = "restored+"; //$NON-NLS-1$
	
	protected static final String TIMESTAMP_SERVER_MERGED = "+modified"; //$NON-NLS-1$
	protected static final String TIMESTAMP_SERVER_MERGED_WITH_CONFLICT = "+="; //$NON-NLS-1$
	
	// a directory sync info will have nothing more than a name
	protected boolean isDirectory = false;
	protected boolean isDeleted = false;
	
	// utility constants
	protected static final String DIRECTORY_PREFIX = "D"; //$NON-NLS-1$
	protected static final String SEPARATOR = "/"; //$NON-NLS-1$
	protected static final byte SEPARATOR_BYTE = (byte)'/';
	
	// fields describing the synchronization of a resource in CVS parlance
	protected String name;
	protected String revision;
	protected Date timeStamp;
	protected KSubstOption keywordMode;
	protected CVSEntryLineTag tag;
	
	// type of sync
	protected int syncType = TYPE_REGULAR;
	protected ResourceSyncInfo() {
		// Added for use by subclasses
	}
	
	public ResourceSyncInfo(byte[] entryLine) throws CVSException {
		this(new String(entryLine), null);
	}
	
	/**
	 * Constructor to create a sync object from entry line formats. The entry lines are parsed by this class.
	 * The constructor can handle parsing entry lines from the server or from an entry file.
	 * 
	 * @param entryLine the entry line (e.g.  /new.java/1.2/Fri Dec 07 00:17:52 2001/-kb/)
	 * @param permissions the file permission (e.g. u=rw,g=rw,o=r). May be <code>null</code>.
	 * @param timestamp if not included in the entry line. May be <code>null</code>.
	 * 
	 * @exception CVSException is thrown if the entry cannot be parsed.
	 */
	public ResourceSyncInfo(String entryLine, Date timestamp) throws CVSException {
		Assert.isNotNull(entryLine);
		setEntryLine(entryLine);
		
		// override the timestamp that may of been in entryLine. In some cases the timestamp is not in the
		// entry line (e.g. receiving entry lines from the server versus reading them from the Entry file).
		if(timestamp!=null) {
			this.timeStamp = timestamp;
		}
	}
	
	/**
	 * Constructor to create a resource sync object for a folder.
	 * 
	 * @param name of the resource for which this sync state is associatied, cannot be <code>null</code>.
	 */
	public ResourceSyncInfo(String name) {
		Assert.isNotNull(name);
		this.name = name;
		this.isDirectory = true;
	}
	/**
	 * Answers if this sync information is for a folder in which case only a name is
	 * available.
	 * 
	 * @return <code>true</code> if the sync information is for a folder and <code>false</code>
	 * if it is for a file.
	 */
	public boolean isDirectory() {
		return isDirectory;
	}
	
	/**
	 * Answers if this sync information is for a resource that has been merged by the cvs server with
	 * conflicts and has not been modified yet relative to the given timestamp.
	 * 
	 * @param otherTimestamp is the timestamp of the file associated with this resource sync
	 * @return <code>true</code> if the sync information is for a file that has been merged and
	 * <code>false</code> for folders and for files that have not been merged.
	 */
	public boolean isNeedsMerge(Date otherTimestamp) {
		return syncType == TYPE_MERGED_WITH_CONFLICTS && timeStamp != null && timeStamp.equals(otherTimestamp);
	}
	
	/**
	 * Answers if this sync information is for a resource that has been merged with conflicts by the 
	 * cvs server.
	 * 
	 * @return <code>true</code> if the sync information is for a file that has been merged and
	 * <code>false</code> for folders and for files that have not been merged.
	 */
	public boolean isMergedWithConflicts() {
		return syncType == TYPE_MERGED_WITH_CONFLICTS;
	}
	
	/**
	 * Answers if this sync information is for a resource that has been merged by the cvs server.
	 * 
	 * @return <code>true</code> if the sync information is for a file that has been merged and
	 * <code>false</code> for folders and for files that have not been merged.
	 */
	public boolean isMerged() {
		return syncType == TYPE_MERGED || isMergedWithConflicts();
	}
	
	/**
	 * Answers if this sync information is for a file that has been added but not comitted
	 * to the CVS repository yet.
	 * 
	 * @return <code>true</code> if the sync information is new or <code>false</code> if 
	 * the sync is for an file that exists remotely. For folder sync info this returns
	 * <code>false</code>.
	 */
	public boolean isAdded() {
		if(!isDirectory) {
			return getRevision().equals(ADDED_REVISION);
		} else {
			return false;
		}
	}
	
	/**
	 * Answers if this sync information is for a file that is scheduled to be deleted
	 * from the repository but the deletion has not yet been committed.
	 * 
	 * @return <code>true</code> if the sync information is deleted or <code>false</code> if 
	 * the sync is for an file that exists remotely.
	 */
	public boolean isDeleted() {
		return isDeleted;
	}
	
	/**
	 * Returns an entry line that can be saved in the CVS/Entries file. For sending entry lines to the
	 * server use <code>getServerEntryLine</code>.
	 * 
	 * @return a file or folder entry line reflecting the state of this sync object.
	 */
	public String getEntryLine() {
		return getEntryLine(true /*include timestamps*/, null /*no timestamp override*/);
	}
		
	/**
	 * Same as <code>getEntryLine</code> except it considers merged files in entry line timestamp format. 
	 * This is only valid for sending the file to the server.
	 * 
	 * @param fileTimestamp is timestamp of the resource associated with this sync info.
	 * @return a file or folder entry line reflecting the state of this sync object.
	 */
	public String getServerEntryLine(Date fileTimestamp) {
		String serverTimestamp;
		if(fileTimestamp != null && (isMerged() || isMergedWithConflicts())) {
			if(isNeedsMerge(fileTimestamp)) {
				serverTimestamp = TIMESTAMP_SERVER_MERGED_WITH_CONFLICT;
			} else {
				serverTimestamp = TIMESTAMP_SERVER_MERGED;
			}
			return getEntryLine(true, serverTimestamp);
		} else {
			return getEntryLine(false, null);
		}		
	}
	
	/**
	 * Gets the tag or <code>null</code> if a tag is not available.
	 * 
	 * @return Returns a String
	 */
	public CVSTag getTag() {
		return tag;
	}
	/**
	 * Gets the timeStamp or <code>null</code> if a timestamp is not available.
	 * 
	 * @return a date instance representing the timestamp
	 */
	public Date getTimeStamp() {
		return timeStamp;
	}
	/**
	 * Gets the version or <code>null</code> if this is a folder sync info. The returned
	 * revision will never include the DELETED_PREFIX. To found out if this sync info is
	 * for a deleted resource call isDeleted().
	 * 
	 * @return Returns a String
	 */
	public String getRevision() {
		return revision;
	}
	
	/**
	 * Gets the name.
	 * 
	 * @return Returns a String
	 */
	public String getName() {
		return name;
	}
	/**
	 * Gets the keyword mode.
	 * @return the keyword substitution option
	 */
	public KSubstOption getKeywordMode() {
		return keywordMode;
	}
	
	/**
	 * Answers the default permissions string.
	 */
	public static String getDefaultPermissions() {
		return DEFAULT_PERMISSIONS;
	}
	
	/**
	 * Answers the default permissions string that is executable.
	 */
	public static String getDefaultExecutablePermissions() {
		return DEFAULT_EXECUTABLE_PERMISSIONS;
	}
	
	/**
	 * Name equality between resource sync info objects.
	 */
	public boolean equals(Object other) {
		if(other instanceof ResourceSyncInfo) {
			ResourceSyncInfo syncInfo = ((ResourceSyncInfo)other);
			if(other == this) return true;
			if(getName() == syncInfo.getName()) return true;
			return getName().equals(syncInfo.getName());
		} else {
			return false;
		}
	}
	
	public int hashCode() {
		return getName().hashCode();
	}
	
	@Override
	public String toString() {
		return getEntryLine(true, null /*no timestamp override*/);
	}
	public MutableResourceSyncInfo cloneMutable() {
		MutableResourceSyncInfo newSync = new MutableResourceSyncInfo(this);
		return newSync;
	}
	/**
	 * Sets the tag for the resource.
	 */
	protected void setTag(CVSTag tag) {
		if(tag!=null) {
			this.tag = new CVSEntryLineTag(tag);
		} else {
			this.tag = null;
		}					
	}
	
		
	/*
	 * Sets the sync type
	 */
	protected void setSyncType(int syncType) {
		this.syncType = syncType;
	}

	/**
	 * Sets the version and decides if the revision is for a deleted resource.
	 * The revision field will not include the deleted prefix '-'. If the
	 * revision has been locked the "locked by" suffix it will be removed from
	 * the revision field.
	 * 
	 * @param revision
	 *            the revision to set
	 * @see #LOCKEDBY_REGEX
	 */
	protected void setRevision(String revision) {
		if(revision==null || revision.equals(ADDED_REVISION)) {
			this.revision = ADDED_REVISION;
			timeStamp = null;
			syncType = TYPE_REGULAR;
			isDeleted = false;
		} else if(revision.startsWith(DELETED_PREFIX)) {
			this.revision = revision.substring(DELETED_PREFIX.length());
			isDeleted = true;
		} else {
			this.revision = revision;
			isDeleted = false;
		}
		this.revision = this.revision.replaceFirst(LOCKEDBY_REGEX, ""); //$NON-NLS-1$
	}
	
	/**
	 * Set the entry line 
	 * 
	 * @throws CVSException if the entryLine is malformed
	 */
	protected void setEntryLine(String entryLine) throws CVSException {

		String[] strings = Util.parseIntoSubstrings(entryLine, SEPARATOR);
		if(strings.length < 6) {
			throw new CVSException(CVSMessages.Malformed_entry_line___11 + entryLine); 
		}
		
		isDirectory = (strings[0].equals(DIRECTORY_PREFIX));
		
		name = strings[1];
		
		if(name.length()==0) {
			throw new CVSException(CVSMessages.Malformed_entry_line__missing_name___12 + entryLine); 
		}
		
		String rev = strings[2];
		
		if(rev.length()==0 && !isDirectory()) {
			throw new CVSException(CVSMessages.Malformed_entry_line__missing_revision___13 + entryLine); 
		} else {
			setRevision(rev);
		}
	
		String date = strings[3];
		
		// possible timestamps are:
		// from server: "+=" and "+modified"
		// from entry line: "Result of Merge+Thu May 25 12:33:33 2002"
		//							 "Result of Merge"
		//							"Thu May 25 12:33:33 2002"
		//
		// The server will send a timestamp of "+=" if
		// the file was merged with conflicts. The '+' indicates that there are conflicts and the
		// '=' indicate that the timestamp for the file should be used. If the merge does not
		// have conflicts, simply add a text only timestamp and the file will be regarded as
		// having outgoing changes.
		// The purpose for having the two different timestamp options for merges is to 
		// dissallow commit of files that have conflicts until they have been manually edited.			
		if(date.contains(ResourceSyncInfo.TIMESTAMP_SERVER_MERGED)) {
			syncType = TYPE_MERGED;
			date = null;
		} else if(date.contains(ResourceSyncInfo.TIMESTAMP_SERVER_MERGED_WITH_CONFLICT)) {
			syncType = TYPE_MERGED_WITH_CONFLICTS;
			date = null;
		} else if(date.contains(TIMESTAMP_MERGED_WITH_CONFLICT)) {
			date = date.substring(date.indexOf("+") + 1); //$NON-NLS-1$
			syncType = TYPE_MERGED_WITH_CONFLICTS;
		} else if(date.contains(TIMESTAMP_MERGED)) {
			syncType = TYPE_MERGED;
			date = null;
		} else if (date.contains(TIMESTAMP_DELETED_AND_RESTORED)) {
			syncType = TYPE_DELETED_AND_RESTORED;
			date = date.substring(date.indexOf("+") + 1); //$NON-NLS-1$
		}
		
		if(date==null || "".equals(date)) { //$NON-NLS-1$
			timeStamp = null;	
		} else {
			try {	
				timeStamp = CVSDateFormatter.entryLineToDate(date);
			} catch(ParseException e) {
				// something we don't understand, just make this sync have no timestamp and
				// never be in sync with the server.
				timeStamp = null;
			}
		}
		keywordMode = KSubstOption.fromMode(strings[4]);
		String tagEntry;
		if (strings.length == 6) {
			tagEntry = strings[5];
		} else {
			// It turns out that CVS supports slashes (/) in the tag even though this breaks the spec
			// See http://dev.eclipse.org/bugs/show_bug.cgi?id=26717
			StringBuilder buffer = new StringBuilder();
			for (int i = 5; i < strings.length; i++) {
				buffer.append(strings[i]);
				if (i < strings.length - 1) {
					buffer.append(SEPARATOR);
				}
			}
			tagEntry = buffer.toString();
		}
						
		if(tagEntry.length()>0) {
			tag = new CVSEntryLineTag(tagEntry);
		} else {
			tag = null;
		}
	}
	
	private String getEntryLine(boolean includeTimeStamp, String timestampOverride) {
		StringBuilder result = new StringBuilder();
		
		if(isDirectory) {
			result.append(DIRECTORY_PREFIX);
			result.append(SEPARATOR);
			result.append(name);
			for (int i = 0; i < 4; i++) {
				result.append(SEPARATOR);
			}
		} else {
			result.append(SEPARATOR);
			result.append(name);
			result.append(SEPARATOR);
			
			if(isDeleted){
				result.append(DELETED_PREFIX); 
			}
			result.append(revision);
			result.append(SEPARATOR);
			if(includeTimeStamp) {
				String entryLineTimestamp = ""; //$NON-NLS-1$
				if(timestampOverride!=null) {
					entryLineTimestamp = timestampOverride;
				} else {					
					switch(syncType) {
						case TYPE_REGULAR:
							if(timeStamp==null) {
								entryLineTimestamp = TIMESTAMP_DUMMY;
							} else {
								entryLineTimestamp = CVSDateFormatter.dateToEntryLine(timeStamp);
							} break;
						case TYPE_MERGED:
							entryLineTimestamp = TIMESTAMP_MERGED; break;
						case TYPE_MERGED_WITH_CONFLICTS:
							entryLineTimestamp = TIMESTAMP_MERGED_WITH_CONFLICT + CVSDateFormatter.dateToEntryLine(timeStamp); break;
						case TYPE_DELETED_AND_RESTORED:
							entryLineTimestamp = TIMESTAMP_DELETED_AND_RESTORED + CVSDateFormatter.dateToEntryLine(timeStamp); break;
					}						
				}
				result.append(entryLineTimestamp);
			}
			result.append(SEPARATOR);
			if (keywordMode != null) result.append(keywordMode.toEntryLineMode());
			result.append(SEPARATOR);
			if (tag != null) {
				result.append(tag.toEntryLineFormat(true));
			}
		}
		return result.toString();
	}
	
	/**
	 * Method getBytes.
	 * @return byte[]
	 */
	public byte[] getBytes() {
		return getEntryLine().getBytes();
	}
	
	/**
	 * Method getName.
	 * @param syncBytes
	 * @return String
	 */
	public static String getName(byte[] syncBytes) throws CVSException {
		String name = Util.getSubstring(syncBytes, SEPARATOR_BYTE, 1, false);
		if (name == null) {
			throw new CVSException(NLS.bind(CVSMessages.ResourceSyncInfo_malformedSyncBytes, new String[] { new String(syncBytes) })); 
		}
		return name;
	}
	
	/**
	 * Method getKeywordMode.
	 * @param syncBytes
	 * @return String
	 */
	public static KSubstOption getKeywordMode(byte[] syncBytes) throws CVSException {
		String mode = Util.getSubstring(syncBytes, SEPARATOR_BYTE, 4, false);
		if (mode == null) {
			throw new CVSException(NLS.bind(CVSMessages.ResourceSyncInfo_malformedSyncBytes, new String[] { new String(syncBytes) })); 
		}
		return KSubstOption.fromMode(mode);
	}
	
	/**
	 * Method setKeywordMode.
	 * 
	 * @param syncBytes
	 *            bytes to set
	 * @param mode
	 *            keyword substitution options
	 * @return modified array of bytes
	 * @throws CVSException
	 *             thrown when the entry lines bytes are malformed
	 */
	public static byte[] setKeywordMode(byte[] syncBytes, KSubstOption mode) throws CVSException {
		return setKeywordMode(syncBytes, mode.toEntryLineMode().getBytes());
	}
	
	/**
	 * Method setKeywordMode.
	 * 
	 * @param syncBytes
	 *            bytes to set
	 * @param modeBytes
	 *            entry line mode bytes
	 * @return modified array of bytes
	 * @throws CVSException
	 *             thrown when the entry lines bytes are malformed
	 */
	public static byte[] setKeywordMode(byte[] syncBytes, byte[] modeBytes) throws CVSException {
		return setSlot(syncBytes, 4, modeBytes);
	}
	
	/**
	 * Return whether the provided syncBytes represent a binary file.
	 * @param syncBytes
	 * @return boolean
	 * @throws CVSException 
	 */
	public static boolean isBinary(byte[] syncBytes)  throws CVSException {
		if (syncBytes == null) return false;
		String mode = Util.getSubstring(syncBytes, SEPARATOR_BYTE, 4, false);
		if (mode == null) {
			throw new CVSException(NLS.bind(CVSMessages.ResourceSyncInfo_malformedSyncBytes, new String[] { new String(syncBytes) })); 
		}
		return "-kb".equals(mode); //$NON-NLS-1$
	}
	
	/**
	 * Method isFolder.
	 * @param syncBytes
	 * @return boolean
	 */
	public static boolean isFolder(byte[] syncBytes) {
		return syncBytes.length > 0 && syncBytes[0] == 'D';
	}

	/**
	 * Method isAddition.
	 * @param syncBytes
	 * @return boolean
	 */
	public static boolean isAddition(byte[] syncBytes) throws CVSException {
		int start = startOfSlot(syncBytes, 2);
		// There must be a slot and, in the very least, there must be two characters after the slot
		if (start == -1 || start > syncBytes.length - 3) {
			throw new CVSException(NLS.bind(CVSMessages.ResourceSyncInfo_malformedSyncBytes, new String[] { new String(syncBytes) })); 
		}
		// If the zero is followed by a dot, then it is a valid revision and not an addition
		return syncBytes[start + 1] == '0' && syncBytes[start + 2] != '.';
	}
	
	/**
	 * Method isDeleted.
	 * @param syncBytes
	 * @return boolean
	 */
	public static boolean isDeletion(byte[] syncBytes) throws CVSException {
		int start = startOfSlot(syncBytes, 2);
		if (start == -1 || start >= syncBytes.length) {
			throw new CVSException(NLS.bind(CVSMessages.ResourceSyncInfo_malformedSyncBytes, new String[] { new String(syncBytes) })); 
		}
		return syncBytes[start + 1] == DELETED_PREFIX_BYTE;
	}
		
	/**
	 * Method convertToDeletion.
	 * @param syncBytes
	 * @return byte[]
	 */
	public static byte[] convertToDeletion(byte[] syncBytes) throws CVSException {
		int index = startOfSlot(syncBytes, 2);
		if (index == -1) {
			throw new CVSException(NLS.bind(CVSMessages.ResourceSyncInfo_malformedSyncBytes, new String[] { new String(syncBytes) })); 
		}
		if (syncBytes.length > index && syncBytes[index+1] != DELETED_PREFIX_BYTE) {
			byte[] newSyncBytes = new byte[syncBytes.length + 1];
			System.arraycopy(syncBytes, 0, newSyncBytes, 0, index + 1);
			newSyncBytes[index + 1] = DELETED_PREFIX_BYTE;
			System.arraycopy(syncBytes, index + 1, newSyncBytes, index + 2, syncBytes.length - index - 1);
			return newSyncBytes;
		}
		return syncBytes;
	}
	
	/**
	 * Method convertFromDeletion.
	 * @param syncBytes
	 * @return byte[]
	 */
	public static byte[] convertFromDeletion(byte[] syncBytes) throws CVSException {
		int index = startOfSlot(syncBytes, 2);
		if (index == -1) {
			throw new CVSException(NLS.bind(CVSMessages.ResourceSyncInfo_malformedSyncBytes, new String[] { new String(syncBytes) })); 
		}
		if (syncBytes.length > index && syncBytes[index+1] == DELETED_PREFIX_BYTE) {
			byte[] newSyncBytes = new byte[syncBytes.length - 1];
			System.arraycopy(syncBytes, 0, newSyncBytes, 0, index + 1);
			System.arraycopy(syncBytes, index + 2, newSyncBytes, index + 1, newSyncBytes.length - index - 1);

			String syncTimestamp = Util.getSubstring(syncBytes, SEPARATOR_BYTE,
					3, false);
			if (getSyncType(new String(syncTimestamp)) == TYPE_REGULAR) {
				syncTimestamp = TIMESTAMP_DELETED_AND_RESTORED + syncTimestamp;
				byte[] oldSyncBytes = newSyncBytes;
				newSyncBytes = new byte[oldSyncBytes.length
						+ TIMESTAMP_DELETED_AND_RESTORED.length()];
				System.arraycopy(oldSyncBytes, 0, newSyncBytes, 0,
						startOfSlot(oldSyncBytes, 3) + 1);
				System.arraycopy(syncTimestamp.getBytes(), 0, newSyncBytes,
						startOfSlot(oldSyncBytes, 3) + 1,
						syncTimestamp.length());
				System.arraycopy(oldSyncBytes,
						startOfSlot(oldSyncBytes, 4) - 1, newSyncBytes,
						startOfSlot(oldSyncBytes, 3) + syncTimestamp.length(),
						oldSyncBytes.length
								- (startOfSlot(oldSyncBytes, 4) - 1));
			}
			return newSyncBytes;
		}
		return syncBytes;
	}
	/**
	 * Method startOfSlot returns the index of the slash that occurs before the
	 * given slot index. The provided index should be &gt;= 1 which assumes that
	 * slot zero occurs before the first slash.
	 * 
	 * @param syncBytes
	 * @param i
	 * @return int
	 */
	private static int startOfSlot(byte[] syncBytes, int slot) {
		int count = 0;
		for (int j = 0; j < syncBytes.length; j++) {
			if (syncBytes[j] == SEPARATOR_BYTE) {
				count++;
				if (count == slot) return j;
			} 
		}
		return -1;
	}
	
	/**
	 * Method setSlot modifies a given array of bytes representing a line in the
	 * CVS/Entry file.
	 * 
	 * @param syncBytes
	 *            array of bytes representing a line in the CVS/Entry file
	 * @param slot
	 *            number of the slot to modify. Slots are groups separated be
	 *            slashes.
	 * @param newBytes
	 *            array of bytes to be set in the given slot
	 * @return byte[] modified array of bytes
	 * @throws CVSException
	 *             thrown when the entry lines bytes are malformed.
	 */
	public static byte[] setSlot(byte[] syncBytes, int slot, byte[] newBytes) throws CVSException {
		int start = startOfSlot(syncBytes, slot);
		if (start == -1) {
			throw new CVSException(NLS.bind(CVSMessages.ResourceSyncInfo_malformedSyncBytes, new String[] { new String(syncBytes) })); 
		}
		int end = startOfSlot(syncBytes, slot + 1);
		int totalLength = start + 1 + newBytes.length;
		if (end != -1) {
			totalLength += syncBytes.length - end;
		}
		byte[] result = new byte[totalLength];
		System.arraycopy(syncBytes, 0, result, 0, start + 1);
		System.arraycopy(newBytes, 0, result, start + 1, newBytes.length);
		if (end != -1) {
			System.arraycopy(syncBytes, end, result, start + 1 + newBytes.length, syncBytes.length - end);
		}
		return result;
	}

	/**
	 * Return the timestamp portion of the sync info that is to be sent to the
	 * server.
	 * 
	 * @param syncBytes
	 * @param fileTimestamp
	 * @return String
	 */
	public static String getTimestampToServer(byte[] syncBytes, Date fileTimestamp) throws CVSException {
		if(fileTimestamp != null) {
			String syncTimestamp = Util.getSubstring(syncBytes, SEPARATOR_BYTE, 3, false);
			if (syncTimestamp == null) {
				throw new CVSException(NLS.bind(CVSMessages.ResourceSyncInfo_malformedSyncBytes, new String[] { new String(syncBytes) })); 
			}
			int syncType = getSyncType(syncTimestamp);
			if (syncType == TYPE_DELETED_AND_RESTORED) {
				return syncTimestamp.substring(syncTimestamp.indexOf("+") + 1); //$NON-NLS-1$
			} else if (syncType != TYPE_REGULAR) {
				if (syncType == TYPE_MERGED_WITH_CONFLICTS && fileTimestamp.equals(getTimestamp(syncTimestamp))) {
					return TIMESTAMP_SERVER_MERGED_WITH_CONFLICT;
				} else {
					return TIMESTAMP_SERVER_MERGED;
				}
			}
		}	
		return null;
	}
	/**
	 * Method getTimestamp.
	 * @param syncTimestamp
	 * @return Object
	 */
	private static Date getTimestamp(String syncTimestamp) {
		String dateString= syncTimestamp;
		if(syncTimestamp.contains(ResourceSyncInfo.TIMESTAMP_SERVER_MERGED)) {
			dateString = null;
		} else if(syncTimestamp.contains(ResourceSyncInfo.TIMESTAMP_SERVER_MERGED_WITH_CONFLICT)) {
			dateString = null;
		} else if(syncTimestamp.contains(TIMESTAMP_MERGED_WITH_CONFLICT)) {
			dateString = syncTimestamp.substring(syncTimestamp.indexOf("+") + 1); //$NON-NLS-1$
		} else if(syncTimestamp.contains(TIMESTAMP_MERGED)) {
			dateString = null;
		}
		
		if(dateString==null || "".equals(dateString)) { //$NON-NLS-1$
			return null;	
		} else {
			try {	
				return CVSDateFormatter.entryLineToDate(dateString);
			} catch(ParseException e) {
				// something we don't understand, just make this sync have no timestamp and
				// never be in sync with the server.
				return null;
			}
		}
	}
	
	/**
	 * Method getSyncType.
	 * @param syncTimestamp
	 * @return int
	 */
	private static int getSyncType(String date) {
		if(date.contains(ResourceSyncInfo.TIMESTAMP_SERVER_MERGED)) {
			return TYPE_MERGED;
		} else if(date.contains(ResourceSyncInfo.TIMESTAMP_SERVER_MERGED_WITH_CONFLICT)) {
			return TYPE_MERGED_WITH_CONFLICTS;
		} else if(date.contains(TIMESTAMP_MERGED_WITH_CONFLICT)) {
			return TYPE_MERGED_WITH_CONFLICTS;
		} else if(date.contains(TIMESTAMP_MERGED)) {
			return TYPE_MERGED;
		} else if (date.contains(TIMESTAMP_DELETED_AND_RESTORED)) {
			return TYPE_DELETED_AND_RESTORED;
		}
		return TYPE_REGULAR;
	}
	
	/**
	 * Method getTag.
	 * @param syncBytes
	 * @return String
	 */
	public static byte[] getTagBytes(byte[] syncBytes) throws CVSException {
		byte[] tag = Util.getBytesForSlot(syncBytes, SEPARATOR_BYTE, 5, true);
		if (tag == null) {
			throw new CVSException(NLS.bind(CVSMessages.ResourceSyncInfo_malformedSyncBytes, new String[] { new String(syncBytes) })); 
		}
		return tag;
	}
	
	/**
	 * Method setTag.
	 * @param syncBytes
	 * @param tagString
	 * @return byte[]
	 */
	public static byte[] setTag(byte[] syncBytes, byte[] tagBytes) throws CVSException {
		return setSlot(syncBytes, 5, tagBytes);
	}
	
	/**
	 * Method setTag.
	 * @param syncBytes
	 * @param tag
	 * @return ResourceSyncInfo
	 */
	public static byte[] setTag(byte[] syncBytes, CVSTag tag) throws CVSException {
		CVSEntryLineTag entryTag;
		if (tag instanceof CVSEntryLineTag) {
			entryTag = (CVSEntryLineTag)tag;
		} else {
			entryTag = new CVSEntryLineTag(tag);
		}
		return setTag(syncBytes, entryTag.toEntryLineFormat(true).getBytes());
	}
	
	/**
	 * Return revision for the given synchronization bytes. The revision will
	 * not include the deleted prefix '-'. The "locked by" suffix it will not be
	 * included either.
	 * 
	 * @param syncBytes
	 *            the bytes that represent the synchronization information
	 * @return a revision string for the given bytes
	 * @see #LOCKEDBY_REGEX
	 */
	public static String getRevision(byte[] syncBytes) throws CVSException {
		String revision = Util.getSubstring(syncBytes, SEPARATOR_BYTE, 2, false);
		if (revision == null) {
			throw new CVSException(NLS.bind(CVSMessages.ResourceSyncInfo_malformedSyncBytes, new String[] { new String(syncBytes) })); 
		}
		if(revision.startsWith(DELETED_PREFIX)) {
			revision = revision.substring(DELETED_PREFIX.length());
		}
		revision = revision.replaceFirst(LOCKEDBY_REGEX, ""); //$NON-NLS-1$
		return revision;
	}

	/**
	 * Method setRevision.
	 * @param syncBytes
	 * @param revision
	 * @return byte[]
	 */
	public static byte[] setRevision(byte[] syncBytes, String revision) throws CVSException {
		return setSlot(syncBytes, 2, revision.getBytes());
	}
	
	/**
	 * Method isMerge.
	 * @param syncBytes1
	 * @return boolean
	 */
	public static boolean isMerge(byte[] syncBytes) throws CVSException {
		String timestamp = Util.getSubstring(syncBytes, SEPARATOR_BYTE, 3, false);
		if (timestamp == null) {
			throw new CVSException(NLS.bind(CVSMessages.ResourceSyncInfo_malformedSyncBytes, new String[] { new String(syncBytes) })); 
		}
		int syncType = getSyncType(timestamp);
		return syncType == TYPE_MERGED || syncType == TYPE_MERGED_WITH_CONFLICTS;
	}

	/**
	 * Checks if the ResourceSyncInfo was deleted and restored afterwards.
	 * Parses the timestamp in <code>syncBytes</code> and searches for markers.
	 * 
	 * @param syncBytes
	 * @return boolean
	 */
	public static boolean wasDeleted(byte[] syncBytes) throws CVSException {
		String timestamp = Util.getSubstring(syncBytes, SEPARATOR_BYTE, 3,
				false);
		if (timestamp == null) {
			throw new CVSException(NLS.bind(
					CVSMessages.ResourceSyncInfo_malformedSyncBytes,
					new String[] { new String(syncBytes) }));
		}
		int syncType = getSyncType(timestamp);
		return syncType == TYPE_DELETED_AND_RESTORED;
	}

	/**
	 * Method isMerge.
	 * @param syncBytes1
	 * @return boolean
	 */
	public static boolean isMergedWithConflicts(byte[] syncBytes) throws CVSException {
		String timestamp = Util.getSubstring(syncBytes, SEPARATOR_BYTE, 3, false);
		if (timestamp == null) {
			throw new CVSException(NLS.bind(CVSMessages.ResourceSyncInfo_malformedSyncBytes, new String[] { new String(syncBytes) })); 
		}
		int syncType = getSyncType(timestamp);
		return syncType == TYPE_MERGED_WITH_CONFLICTS;
	}
	
	
	/**
	 * Return <code>true</code> if the remoteBytes represents a later revision on the same
	 * branch as localBytes. Return <code>false</code> if remoteBytes is the same or an earlier 
	 * revision or if the bytes are on a separate branch (or tag)
	 * @param remoteBytes
	 * @param localBytes
	 * @return
	 */
	public static boolean isLaterRevisionOnSameBranch(byte[] remoteBytes, byte[] localBytes) throws CVSException {
		// If the two byte arrays are the same, then the remote isn't a later revision
		if (remoteBytes == localBytes) return false;
		//	If the tags differ, then the remote isn't a later revision
		byte[] remoteTag = ResourceSyncInfo.getTagBytes(remoteBytes);
		byte[] localTag = ResourceSyncInfo.getTagBytes(localBytes);
		if (!Util.equals(remoteTag, localTag)) return false;
		// If the revisions are the same, the remote isn't later
		String remoteRevision = ResourceSyncInfo.getRevision(remoteBytes);
		String localRevision = ResourceSyncInfo.getRevision(localBytes);
		if (remoteRevision.equals(localRevision)) return false;
		return isLaterRevision(remoteRevision, localRevision);
	}

	/**
	 * Return true if the remoteRevision represents a later revision than the local revision
	 * on the same branch.
	 * @param remoteRevision
	 * @param localRevision
	 * @return
	 */
	public static boolean isLaterRevision(String remoteRevision, String localRevision) {
		int localDigits[] = Util.convertToDigits(localRevision);
		if (localDigits.length == 0) return false;
		int remoteDigits[] = Util.convertToDigits(remoteRevision);
		if (remoteDigits.length == 0) return false;
		
		if (localRevision.equals(ADDED_REVISION)) {
			return (remoteDigits.length >= 2);
		}
		if (localDigits.length < remoteDigits.length) {
			// If there are more digits in the remote revision then all
			// the leading digits must match
			for (int i = 0; i < localDigits.length; i++) {
				int localDigit = localDigits[i];
				int remoteDigit = remoteDigits[i];
				if (remoteDigit != localDigit) return false;
			}
			return true;
		}
		// They are the same length or the local is longer.
		// The last digit must differ and all others must be the same.
		// If the local is longer, ignore the addition numbers
		// (this can occur as the result on an import)
		for (int i = 0; i < remoteDigits.length - 1; i++) {
			int localDigit = localDigits[i];
			int remoteDigit = remoteDigits[i];
			if (remoteDigit != localDigit) return false;
		}
		// All the leading digits are equals so the remote is later if the last digit is greater
		return localDigits[remoteDigits.length - 1] < remoteDigits[remoteDigits.length - 1] ;
	}
}
