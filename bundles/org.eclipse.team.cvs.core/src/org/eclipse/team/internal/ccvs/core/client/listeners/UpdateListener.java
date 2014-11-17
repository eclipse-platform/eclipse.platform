/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.client.listeners;

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.client.CommandOutputListener;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.core.util.Util;

public class UpdateListener extends CommandOutputListener {

	// Pattern matchers
	private static ServerMessageLineMatcher MERGED_BINARY_FILE_LINE_1;
	private static ServerMessageLineMatcher MERGED_BINARY_FILE_LINE_2;
	
	// Pattern Variables
	private static final String REVISION_VARIABLE_NAME = "revision"; //$NON-NLS-1$
	private static final String LOCAL_FILE_PATH_VARIABLE_NAME = "localFilePath"; //$NON-NLS-1$
	private static final String BACKUP_FILE_VARIABLE_NAME = "backupFile"; //$NON-NLS-1$
	
	static {
		try {
			String line1 = "revision " //$NON-NLS-1$
				+ Util.getVariablePattern(IMessagePatterns.REVISION_PATTERN, REVISION_VARIABLE_NAME)
				+ " from repository is now in "  //$NON-NLS-1$
				+ Util.getVariablePattern(IMessagePatterns.FILE_PATH_PATTERN, LOCAL_FILE_PATH_VARIABLE_NAME);
			MERGED_BINARY_FILE_LINE_1 = new ServerMessageLineMatcher(
					line1, 
					new String[] {REVISION_VARIABLE_NAME, LOCAL_FILE_PATH_VARIABLE_NAME});
			String line2 = "file from working directory is now in " //$NON-NLS-1$
				+ Util.getVariablePattern(IMessagePatterns.REVISION_PATTERN, BACKUP_FILE_VARIABLE_NAME);
			MERGED_BINARY_FILE_LINE_2 = new ServerMessageLineMatcher(
					line2, 
					new String[] {BACKUP_FILE_VARIABLE_NAME});

		} catch (CVSException e) {
			// Shouldn't happen
			CVSProviderPlugin.log(e);
		}
	}
	
	IUpdateMessageListener updateMessageListener;
	boolean merging = false;
	boolean mergingBinary = false;
	String mergedBinaryFileRevision, mergedBinaryFilePath;

	public UpdateListener(IUpdateMessageListener updateMessageListener) {
		this.updateMessageListener = updateMessageListener;
	}
	
	public IStatus messageLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot,
		IProgressMonitor monitor) {
		mergingBinary = false;
		if (updateMessageListener == null) return OK;
		if(line.startsWith("Merging differences")) { //$NON-NLS-1$
			merging = true;
		} else if(line.indexOf(' ')==1) {
			// We have a message that indicates the type of update. The possible messages are
			// defined by the prefix constants MLP_*.
			String path = line.substring(2);
			char changeType = line.charAt(0);
			
			// calculate change type
			int type = 0;
			switch(changeType) {
				case 'A': type = Update.STATE_ADDED_LOCAL; break; // new file locally that was added but not comitted to server yet
				case '?': type = Update.STATE_UNKOWN; break; // new file locally but not added to server
				case 'U': type = Update.STATE_REMOTE_CHANGES; break;  // remote changes to an unmodified local file
				case 'R': type = Update.STATE_DELETED; break; // removed locally but still exists on the server
				case 'M': type = Update.STATE_MODIFIED; break; // modified locally
				case 'C': type = Update.STATE_CONFLICT; break;  // modified locally and on the server but cannot be auto-merged
				case 'D': type = Update.STATE_DELETED; break;  // deleted locally but still exists on server
				default: type = Update.STATE_NONE;
			}
				
			if (merging) {
				// If we are merging the modified prefix is used both to show merges and
				// local changes. We have to detect this case and use a more specific change
				// type.
				if (type == Update.STATE_MODIFIED)
					type = Update.STATE_MERGEABLE_CONFLICT;
				merging = false;
			}
			updateMessageListener.fileInformation(type, commandRoot, path);
		}
		return OK;
	}

	/**
	 * This handler is used by the RemoteResource hierarchy to retrieve E messages
	 * from the CVS server in order to determine the folders contained in a parent folder.
	 * 
	 * WARNING: This class parses the message output to determine the state of files in the
	 * repository. Unfortunately, these messages seem to be customizable on a server by server basis.
	 * 
	 * Here's a list of responses we expect in various situations:
	 * 
	 * Directory exists remotely:
	 *    cvs server: Updating folder1/folder2
	 * Directory doesn't exist remotely:
	 *    cvs server: skipping directory folder1/folder2
	 * New (or unknown) remote directory
	 *    cvs server: New Directory folder1/folder2
	 * File removed remotely
	 *    cvs server: folder1/file.ext is no longer in the repository
	 *    cvs server: warning: folder1/file.ext is not (any longer) pertinent
	 * Locally added file was added remotely as well
	 *    cvs server: conflict: folder/file.ext created independently by second party 
	 * File removed locally and modified remotely
	 *    cvs server: conflict: removed file.txt was modified by second party
	 * File modified locally but removed remotely
	 *    cvs server: conflict: file.txt is modified but no longer in the repository
	 * Ignored Messages
	 *    cvs server: cannot open directory ...
	 *    cvs server: nothing known about ...
	 * Tag error that really means there are no files in a directory
	 *    cvs [server aborted]: no such tag
	 * Merge contained conflicts
	 *    rcsmerge: warning: conflicts during merge
	 * Binary file conflict
	 *    cvs server: nonmergeable file needs merge
	 *    cvs server: revision 1.4 from repository is now in a1/a2/test
	 *    cvs server: file from working directory is now in .#test.1.3
	 */
	public IStatus errorLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot,
		IProgressMonitor monitor) {
		
		try {
			// Reset flag globally here because we have to many exit points
			boolean wasMergingBinary = mergingBinary;
			mergingBinary = false;
			String serverMessage = getServerMessage(line, location);
			if (serverMessage != null) {
				// Strip the prefix from the line
				String message = serverMessage;
				if (message.startsWith("Updating")) { //$NON-NLS-1$
					if (updateMessageListener != null) {
						String path = message.substring(9);
						updateMessageListener.directoryInformation(commandRoot, path, false);
					}
					return OK;
				} else if (message.startsWith("skipping directory")) { //$NON-NLS-1$
					if (updateMessageListener != null) {
						String path = message.substring(18).trim();
						updateMessageListener.directoryDoesNotExist(commandRoot, path);
					}
					return OK;
				} else if (message.startsWith("New directory")) { //$NON-NLS-1$
					if (updateMessageListener != null) {
						String path = message.substring(15, message.lastIndexOf('\''));
						updateMessageListener.directoryInformation(commandRoot, path, true);
					}
					return OK;
				} else if (message.endsWith("is no longer in the repository")) { //$NON-NLS-1$
					if (updateMessageListener != null) {
						String filename = message.substring(0, message.length() - 31);
						filename = stripQuotes(filename);
						updateMessageListener.fileDoesNotExist(commandRoot, filename);
					}
					return OK;
				} else if (message.startsWith("conflict:")) { //$NON-NLS-1$
					/*
					 * We can get the following conflict warnings
					 *    cvs server: conflict: folder/file.ext created independently by second party 
					 *    cvs server: conflict: removed file.txt was modified by second party
					 *    cvs server: conflict: file.txt is modified but no longer in the repository
					 * If we get the above line, we have conflicting additions or deletions and we can expect a server error.
					 * We still get "C foler/file.ext" so we don't need to do anything else (except in the remotely deleted case)
					 */
					if (updateMessageListener != null) {
						if (message.endsWith("is modified but no longer in the repository")) { //$NON-NLS-1$
							// The "C foler/file.ext" will come after this so if whould be ignored!
							String filename = message.substring(10, message.length() - 44);
							filename = stripQuotes(filename);
							updateMessageListener.fileDoesNotExist(commandRoot, filename);
						}
					}
					return new CVSStatus(IStatus.WARNING, CVSStatus.CONFLICT, line, commandRoot);
				} else if (message.startsWith("warning:")) { //$NON-NLS-1$
					/*
					 * We can get the following conflict warnings
					 *    cvs server: warning: folder1/file.ext is not (any longer) pertinent
					 * If we get the above line, we have local changes to a remotely deleted file.
					 */
					if (updateMessageListener != null) {
						if (message.endsWith("is not (any longer) pertinent")) { //$NON-NLS-1$
							String filename = message.substring(9, message.length() - 30);
							updateMessageListener.fileDoesNotExist(commandRoot, filename);
						}
					}
					return new CVSStatus(IStatus.WARNING, CVSStatus.CONFLICT, line, commandRoot);
				} else if (message.startsWith("conflicts")) { //$NON-NLS-1$
					// This line is info only. The server doesn't report an error.
					return new CVSStatus(IStatus.INFO, CVSStatus.CONFLICT, line, commandRoot);
				} else if (message.startsWith("nonmergeable file needs merge")) { //$NON-NLS-1$
					mergingBinary = true;
					mergedBinaryFileRevision = null;
					mergedBinaryFilePath = null;
					return OK;
				} else if (wasMergingBinary) {
					Map variables = MERGED_BINARY_FILE_LINE_1.processServerMessage(message);
					if (variables != null) {
						mergedBinaryFileRevision = (String)variables.get(REVISION_VARIABLE_NAME);
						mergedBinaryFilePath = (String)variables.get(LOCAL_FILE_PATH_VARIABLE_NAME);
						mergingBinary = true;
						return OK;
					}
					variables = MERGED_BINARY_FILE_LINE_2.processServerMessage(message);
					if (variables != null) {
						String backupFile = (String)variables.get(BACKUP_FILE_VARIABLE_NAME);
						try {
							if (mergedBinaryFileRevision != null && mergedBinaryFilePath != null) {
								ICVSFile file = commandRoot.getFile(mergedBinaryFilePath);
								IResource resource = file.getIResource();
								if (resource != null) {
									return new CVSStatus(IStatus.ERROR, CVSStatus.UNMEGERED_BINARY_CONFLICT,
										NLS.bind(CVSMessages.UpdateListener_0, (new Object[] { 
                                        resource.getFullPath().toString(), 
                                        mergedBinaryFileRevision, 
                                        resource.getFullPath().removeLastSegments(1).append(backupFile).toString()})), commandRoot);
								}
							}
						} catch (CVSException e1) {
							CVSProviderPlugin.log(e1);
						}
						return OK;
					}
				}
				
				// Fallthrough case for "cvs server" messages
				if (!message.startsWith("cannot open directory") //$NON-NLS-1$
						&& !message.startsWith("nothing known about")) { //$NON-NLS-1$
					return super.errorLine(line, location, commandRoot, monitor);
				} 
			} else {
				String serverAbortedMessage = getServerAbortedMessage(line, location);
				if (serverAbortedMessage != null) {
					// Strip the prefix from the line
					String message = serverAbortedMessage;
					if (message.startsWith("no such tag")) { //$NON-NLS-1$
						// This is reported from CVS when a tag is used on the update there are no files in the directory
						// To get the folders, the update request should be re-issued for HEAD
						return new CVSStatus(IStatus.WARNING, CVSStatus.NO_SUCH_TAG, line, commandRoot);
					} else if (message.startsWith("Numeric join") && message.endsWith("may not contain a date specifier")) { //$NON-NLS-1$ //$NON-NLS-2$
					    // This error indicates a join failed because a date tag was used
					    return super.errorLine(line, location, commandRoot, monitor);
					} else {
						return super.errorLine(line, location, commandRoot, monitor);
					}
				} else if (line.equals("rcsmerge: warning: conflicts during merge")) { //$NON-NLS-1$
					// There were conflicts in the merge
					return new CVSStatus(IStatus.WARNING, CVSStatus.CONFLICT, line, commandRoot);
				}
			}
		} catch (StringIndexOutOfBoundsException e) {
			// Something went wrong in the parsing of the message.
			// Return a status indicating the problem
			if (Policy.DEBUG) {
				System.out.println("Error parsing E line: " + line); //$NON-NLS-1$
			}
			return new CVSStatus(IStatus.ERROR, CVSStatus.ERROR_LINE_PARSE_FAILURE, line, commandRoot);
		}
		return super.errorLine(line, location, commandRoot, monitor);
	}

	private String stripQuotes(String filename) {
		// CVS version 12 fix - filenames are returned inside quotes
		// Fixes bug 49056
		if (filename.startsWith("`") && filename.endsWith("'")) //$NON-NLS-1$ //$NON-NLS-2$
			filename = filename.substring(1,filename.length()-1);
		return filename;
	}

}
