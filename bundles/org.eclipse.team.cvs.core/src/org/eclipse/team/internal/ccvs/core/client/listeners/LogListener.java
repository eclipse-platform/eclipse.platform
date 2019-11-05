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
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 179977 CVS log command doesn't scale well with lots of tags and versions
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 194396 Reduce retained memory usage of LogEntry objects
 *     Olexiy Buyanskyy <olexiyb@gmail.com> - Bug 76386 - [History View] CVS Resource History shows revisions from all branches
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.client.listeners;

import java.text.*;
import java.util.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.ITag;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.CommandOutputListener;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * Log listener that parses the log entries returned from the
 * server but delegates the handling of the entries to a subclass.
 */
public class LogListener extends CommandOutputListener {
	
	/*
	 * A new format for log dates was introduced in 1.12.9
	 */
	private static final String LOG_TIMESTAMP_FORMAT_OLD= "yyyy/MM/dd HH:mm:ss zzz";//$NON-NLS-1$
	private static final String LOG_TIMESTAMP_FORMAT= "yyyy-MM-dd HH:mm:ss zzz";//$NON-NLS-1$
	private static final Locale LOG_TIMESTAMP_LOCALE= Locale.US;
	private final DateFormat LOG_DATE_FORMATTER_OLD = new SimpleDateFormat(LOG_TIMESTAMP_FORMAT_OLD, LOG_TIMESTAMP_LOCALE);
	private final DateFormat LOG_DATE_FORMATTER = new SimpleDateFormat(LOG_TIMESTAMP_FORMAT, LOG_TIMESTAMP_LOCALE);
	
	// Server message prefix used for error detection
	private static final String NOTHING_KNOWN_ABOUT = "nothing known about "; //$NON-NLS-1$

	// States of log accumulation.
	private final int DONE = 4;
	private final int COMMENT = 3;
	private final int REVISION = 2;
	private final int SYMBOLIC_NAMES = 1;
	private final int BEGIN = 0;
	
	//Tag used for accumulating all of a branch's revision info
	public final static String BRANCH_REVISION = "branchRevision"; //$NON-NLS-1$
	
	private static final CVSTag[] NO_TAGS = new CVSTag[0];
	private static final String[] NO_VERSIONS = new String[0];
	
	// Instance variables for accumulating Log information
	private RemoteFile currentFile;
	private int state = BEGIN;
	private StringBuffer comment;
	private String fileState;
	private String revision;
	private String author;
	private Date creationDate;
	private List<VersionInfo> versions = new ArrayList<>();
	private Map<String,String> internedStrings = new HashMap<>();
	private final ILogEntryListener listener;
	
	/**
	 * Create a log listener for receiving entries for one or more files.
	 */
	public LogListener(ILogEntryListener listener) {
		this.listener = listener;
	}

	public LogListener(RemoteFile file, ILogEntryListener listener) {
		this(listener);
		this.currentFile = file;   
	}

	private String getRelativeFilePath(ICVSRepositoryLocation location, String fileName) {
		if (fileName.endsWith(",v")) { //$NON-NLS-1$
			fileName = fileName.substring(0, fileName.length() - 2);
		}
		fileName = Util.removeAtticSegment(fileName);
		String rootDirectory = location.getRootDirectory();
		if (fileName.startsWith(rootDirectory)) {
			try {
				fileName = Util.getRelativePath(rootDirectory, fileName);
			} catch (CVSException e) {
				CVSProviderPlugin.log(e);
				return null;
			}
		}
		return fileName;
	}

	public IStatus errorLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot, IProgressMonitor monitor) {
		String serverMessage = getServerMessage(line, location);
		if (serverMessage != null) {
			// look for the following condition
			// E cvs server: nothing known about fileName
			if (serverMessage.startsWith(NOTHING_KNOWN_ABOUT)) {
				return new CVSStatus(IStatus.ERROR, CVSStatus.DOES_NOT_EXIST, line, commandRoot);
			}
		}
		return OK;
	}

	public IStatus messageLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot, IProgressMonitor monitor) {
		// Fields we will find in the log for a file
		// keys = String (tag name), values = String (tag revision number) */
		switch (state) {
			case BEGIN:
				if (line.startsWith("RCS file: ")) { //$NON-NLS-1$
					// We are starting to recieve the log for a file
					String fileName = getRelativeFilePath(location, line.substring(10).trim());
					if (fileName == null) {
						currentFile = null;
						handleInvalidFileName(location, fileName);
					} else {
						if (currentFile == null || !currentFile.getRepositoryRelativePath().equals(fileName)) {
							// We are starting another file
							beginFile(location, fileName);
						}
					}
				} else  if (line.startsWith("symbolic names:")) { //$NON-NLS-1$
					state = SYMBOLIC_NAMES;
				} else if (line.startsWith("revision ")) { //$NON-NLS-1$
					// if the revision has been locked, remove the "locked by" suffix 
					revision = line.substring(9).replaceFirst(ResourceSyncInfo.LOCKEDBY_REGEX, ""); //$NON-NLS-1$
					revision = internAndCopyString(revision);
					state = REVISION;
				} else if (line.startsWith("total revisions:")){ //$NON-NLS-1$
					//if there are no current revision selected and this is a branch then we are in the 
					//case where there have been no changes made on the branch since the initial branching
					//and we need to get the revision that the branch was made from
					int indexOfSelectedRevisions = line.lastIndexOf("selected revisions: "); //$NON-NLS-1$
					//20 for length of "selected revisions: "
					String selectedRevisions = line.substring(indexOfSelectedRevisions + 20).trim();
					if (selectedRevisions.equals("0")){ //$NON-NLS-1$
						//ok put into comment state to await ======= and add info to log
						state = COMMENT;
						revision = BRANCH_REVISION;
						comment = new StringBuffer();
					}
				}
				break;
			case SYMBOLIC_NAMES:
				if (line.startsWith("keyword substitution:")) { //$NON-NLS-1$
					state = BEGIN;
				} else {
					int firstColon = line.indexOf(':');
					String tagName = internAndCopyString(line.substring(1, firstColon));
					String tagRevision = internAndCopyString(line.substring(firstColon + 2));
					versions.add(new VersionInfo(tagRevision, tagName));
				}
				break;
			case REVISION:
				// date: 2000/06/19 04:56:21;  author: somebody;  state: Exp;  lines: +114 -45
				// get the creation date
				int endOfDateIndex = line.indexOf(';', 6);
				creationDate = convertFromLogTime(line.substring(6, endOfDateIndex) + " GMT"); //$NON-NLS-1$
	
				// get the author name
				int endOfAuthorIndex = line.indexOf(';', endOfDateIndex + 1);
				author = internAndCopyString(line.substring(endOfDateIndex + 11, endOfAuthorIndex));

				// get the file state (because this revision might be "dead")
				int endOfStateIndex = line.indexOf(';', endOfAuthorIndex + 1) < 0 ? line.length() : line.indexOf(';', endOfAuthorIndex + 1);
				fileState = internAndCopyString(line.substring(endOfAuthorIndex + 10, endOfStateIndex));
				comment = new StringBuffer();
				state = COMMENT;
				break;
			case COMMENT:
				// skip next line (info about branches) if it exists, if not then it is a comment line.
				if (line.startsWith("branches:")) break; //$NON-NLS-1$
				if (line.equals("=============================================================================") //$NON-NLS-1$
					|| line.equals("----------------------------")) { //$NON-NLS-1$
					state = DONE;
					break;
				}
				//check for null if we are in the waiting to finish case (brought on by branches)
				if (comment == null)
					break; 
				
				if (comment.length() != 0) comment.append('\n');
				comment.append(line);
				break;
		}
		if (state == DONE) {
			// we are only interested in tag names for this revision, remove all others.
			List<ITag> thisRevisionTags = versions.isEmpty() ? Collections.emptyList() : new ArrayList<>(3);
			List<ITag> thisRevisionBranches = new ArrayList<>(1);
			//a parallel lists for revision tags (used only for branches with no commits on them)
			List<String> revisionVersions = versions.isEmpty() ? Collections.emptyList() : new ArrayList<>(3);
			String branchRevision = this.getBranchRevision(revision);
			for (Iterator i = versions.iterator(); i.hasNext();) {
				VersionInfo version = (VersionInfo) i.next();
				String tagName = version.getTagName();
				String tagRevision = version.getTagRevision();
				String tagBranchRevision = version.getBranchRevision();
				int type = version.isBranch() ? CVSTag.BRANCH : CVSTag.VERSION;
				if ( branchRevision.equals(tagBranchRevision) || 
						(version.isBranch() && revision.equals(tagRevision))) {
					CVSTag cvsTag = new CVSTag(tagName, tagBranchRevision, type);
					thisRevisionBranches.add(cvsTag);
				}
				
				if (tagRevision.equals(revision) ||
					revision.equals(BRANCH_REVISION)) {
					CVSTag cvsTag = new CVSTag(tagName, tagBranchRevision, type);
					thisRevisionTags.add(cvsTag);
					if (revision.equals(BRANCH_REVISION)){
						//also record the tag revision
						revisionVersions.add(tagRevision);
					}
				}
			}
			
			if (branchRevision.equals(CVSTag.HEAD_REVISION)) {
				CVSTag tag = new CVSTag(CVSTag.HEAD_BRANCH, CVSTag.HEAD_REVISION, CVSTag.HEAD);
				thisRevisionBranches.add(tag);
			} else {
				if ( thisRevisionBranches.isEmpty()) {
					CVSTag cvsTag = new CVSTag(CVSTag.UNKNOWN_BRANCH, branchRevision, CVSTag.BRANCH);
					thisRevisionBranches.add(cvsTag);
				}			
			}
			if (currentFile != null) {
				LogEntry entry = new LogEntry(currentFile, revision, author, creationDate,
					internString(comment.toString()), fileState, 
					!thisRevisionTags.isEmpty() ? (CVSTag[]) thisRevisionTags.toArray(new CVSTag[thisRevisionTags.size()]) :NO_TAGS, 
					!thisRevisionBranches.isEmpty() ? (CVSTag[]) thisRevisionBranches.toArray(new CVSTag[thisRevisionBranches.size()]) :NO_TAGS, 
						!revisionVersions.isEmpty() ? (String[]) revisionVersions.toArray(new String[revisionVersions.size()]) : NO_VERSIONS);
				addEntry(entry);
			}
			state = BEGIN;
		}
		return OK;
	}

	/**
	 * Convert revision number to branch number.
	 * 
	 * <table border="1">
	 * <tr><th>revision</th><th>branch</th><th>comment</th></tr>
	 * <tr><td>1.1.2.1</td><td>1.1.0.2</td><td>regular branch</td></tr>
	 * <tr><td>1.1.4.1</td><td>1.1.0.4</td><td>regular branch</td></tr>
	 * <tr><td>1.1.1.2</td><td>1.1.1</td><td>vendor branch</td></tr>
	 * <tr><td>1.1.2.1.2.3</td><td>1.1.2.1.0.2</td><td>branch created from another branch</td></tr>
	 * </table>
	 * 
	 * @param revision revision number
	 * @return branch number
	 * 
	 */
	private String getBranchRevision(String revision) {
		if (revision.length() == 0 || !revision.contains(".")) //$NON-NLS-1$
			throw new IllegalArgumentException(
					"Revision malformed: " + revision); //$NON-NLS-1$
		String branchNumber = revision.substring(0, revision.lastIndexOf(".")); //$NON-NLS-1$
		if (!branchNumber.contains(".") || branchNumber.equals(CVSTag.VENDOR_REVISION)) { //$NON-NLS-1$
			return branchNumber;
		}
		String branchPrefix = branchNumber.substring(0,
				branchNumber.lastIndexOf(".")); //$NON-NLS-1$
		branchPrefix += ".0"; //$NON-NLS-1$
		branchPrefix += branchNumber.substring(branchNumber.lastIndexOf(".")); //$NON-NLS-1$
		return branchPrefix;
	}
	
	protected void beginFile(ICVSRepositoryLocation location, String fileName) {
		currentFile = RemoteFile.create(fileName, location);
		versions.clear();
	}

	protected void addEntry(LogEntry entry) {
		listener.handleLogEntryReceived(entry);
	}

	protected void handleInvalidFileName(ICVSRepositoryLocation location, String badFilePath) {
		CVSProviderPlugin.log(IStatus.WARNING, "Invalid file path '" + badFilePath + "' received from " + location.toString(), null); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Converts a time stamp as sent from a cvs server for a "log" command into a
	 * <code>Date</code>.
	 */
	private Date convertFromLogTime(String modTime) {
		DateFormat format = LOG_DATE_FORMATTER;
		// Compatibility for older cvs version (pre 1.12.9)
		if (modTime.length() > 4 && modTime.charAt(4) == '/')
			format = LOG_DATE_FORMATTER_OLD;
		
		try {
			return format.parse(modTime);
		} catch (ParseException e) {
			// fallback is to return null
			return null;
		}
	}
	
	private String internAndCopyString(String string) {
		String internedString = internedStrings.get(string);
		if (internedString == null) {
			internedString = new String(string);
			internedStrings.put(internedString, internedString);
		}
		return internedString;
	}
	
	private String internString(String string) {
		String internedString = internedStrings.get(string);
		if (internedString == null) {
			internedString = string;
			internedStrings.put(internedString, internedString);
		}
		return internedString;    	
	}
	
	private static class VersionInfo {
		private final boolean isBranch;
		private String tagRevision;
		private String branchRevision;
		private final String tagName;
		
		public VersionInfo(String version, String tagName) {
			this.tagName = tagName;
			this.isBranch = isBranchTag(version);
			tagRevision = version;
			if (isBranch) {
				int lastDot = version.lastIndexOf('.');
				if (lastDot == -1) {
					CVSProviderPlugin.log(IStatus.ERROR, 
						NLS.bind(CVSMessages.LogListener_invalidRevisionFormat, new String[] { tagName, version }), null); 
				} else {
					if (version.charAt(lastDot - 1) == '0' && version.charAt(lastDot - 2) == '.') {
						lastDot = lastDot - 2;
					}
					this.branchRevision = version;
					tagRevision = version.substring(0, lastDot);
				}
			}
		}

		public String getTagName() {
			return this.tagName;
		}
		
		public String getTagRevision() {
			return this.tagRevision;
		}
		
		public boolean isBranch() {
			return isBranch;
		}
		
		/** branch tags have odd number of segments or have
		 *  an even number with a zero as the second last segment
		 *  e.g: 1.1.1, 1.26.0.2 are branch revision numbers */
		private boolean isBranchTag(String tagName) {
			// First check if we have an odd number of segments (i.e. even number of dots)
			int numberOfDots = 0;
			int lastDot = 0;
			for (int i = 0; i < tagName.length(); i++) {
				if (tagName.charAt(i) == '.') {
					numberOfDots++;
					lastDot = i;
				}
			}
			if ((numberOfDots % 2) == 0) return true;
			if (numberOfDots == 1) return false;
			
			// If not, check if the second lat segment is a zero
			if (tagName.charAt(lastDot - 1) == '0' && tagName.charAt(lastDot - 2) == '.') return true;
			return false;
		}

		public String getBranchRevision() {
			return branchRevision;
		}
	}
}
