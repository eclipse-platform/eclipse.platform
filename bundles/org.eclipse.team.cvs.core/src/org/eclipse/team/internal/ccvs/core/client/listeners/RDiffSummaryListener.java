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
package org.eclipse.team.internal.ccvs.core.client.listeners;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.client.CommandOutputListener;

/**
 * This class parses the messages recieved in response to an "cvs rdiff -s ..." command
 */
public class RDiffSummaryListener extends CommandOutputListener {

	private static final String RIGHT_REVISION_VARIABLE_NAME = "rightRevision"; //$NON-NLS-1$
	private static final String LEFT_REVISION_VARIABLE_NAME = "leftRevision"; //$NON-NLS-1$
	private static final String REMOTE_FILE_PATH_VARIABLE_NAME = "remoteFilePath"; //$NON-NLS-1$
	private static final String REMOTE_FOLDER_PATH_VARIABLE_NAME = "remoteFolderPath"; //$NON-NLS-1$
	
	private IFileDiffListener listener;
	private static ServerMessageLineMatcher DIRECTORY_MATCHER;
	private static ServerMessageLineMatcher FILE_DIFF_MATCHER;
	private static ServerMessageLineMatcher NEW_FILE_MATCHER;
	private static ServerMessageLineMatcher DELETED_FILE_MATCHER;
	private static ServerMessageLineMatcher DELETED_FILE_MATCHER2;
	
	static {
		// TODO: temprary until proper lifecycle is defined
		initializePatterns();
	}
	public static void initializePatterns() {
		try {
			DIRECTORY_MATCHER = new ServerMessageLineMatcher(
				IMessagePatterns.RDIFF_DIRECTORY, new String[] {REMOTE_FOLDER_PATH_VARIABLE_NAME});
			FILE_DIFF_MATCHER = new ServerMessageLineMatcher(
				IMessagePatterns.RDIFF_SUMMARY_FILE_DIFF, new String[] {REMOTE_FILE_PATH_VARIABLE_NAME, LEFT_REVISION_VARIABLE_NAME, RIGHT_REVISION_VARIABLE_NAME});
			NEW_FILE_MATCHER = new ServerMessageLineMatcher(
				IMessagePatterns.RDIFF_SUMMARY_NEW_FILE, new String[] {REMOTE_FILE_PATH_VARIABLE_NAME, RIGHT_REVISION_VARIABLE_NAME});
			DELETED_FILE_MATCHER = new ServerMessageLineMatcher(
				IMessagePatterns.RDIFF_SUMMARY_DELETED_FILE, new String[] {REMOTE_FILE_PATH_VARIABLE_NAME});
			DELETED_FILE_MATCHER2 = new ServerMessageLineMatcher(
					IMessagePatterns.RDIFF_SUMMARY_DELETED_FILE2, new String[] {REMOTE_FILE_PATH_VARIABLE_NAME, LEFT_REVISION_VARIABLE_NAME});
		} catch (CVSException e) {
			// This is serious as the listener will not function properly
			CVSProviderPlugin.log(e);
		}
	}
	
	public interface IFileDiffListener {
		public void fileDiff(
				String remoteFilePath,
				String leftRevision,
				String rightRevision);
		public void newFile(
				String remoteFilePath,
				String rightRevision);
		public void deletedFile(
				String remoteFilePath, 
				String leftRevision);
		public void directory(String remoteFolderPath);
	}
	
	public RDiffSummaryListener(IFileDiffListener listener) {
		this.listener = listener;
	}
	
	@Override
	public IStatus messageLine(
		String line,
		ICVSRepositoryLocation location,
		ICVSFolder commandRoot,
		IProgressMonitor monitor) {
		
		Map variables = FILE_DIFF_MATCHER.processServerMessage(line);
		if (variables != null) {
			listener.fileDiff(
					(String)variables.get(REMOTE_FILE_PATH_VARIABLE_NAME), 
					(String)variables.get(LEFT_REVISION_VARIABLE_NAME), 
					(String)variables.get(RIGHT_REVISION_VARIABLE_NAME));
			return OK;
		}
		
		variables = NEW_FILE_MATCHER.processServerMessage(line);
		if (variables != null) {
			listener.newFile(
					(String)variables.get(REMOTE_FILE_PATH_VARIABLE_NAME), 
					(String)variables.get(RIGHT_REVISION_VARIABLE_NAME));
			return OK;
		}
		
		variables = DELETED_FILE_MATCHER.processServerMessage(line);
		if (variables != null) {
			listener.deletedFile(
					(String)variables.get(REMOTE_FILE_PATH_VARIABLE_NAME),
					null);
			return OK;
		}
		
		variables = DELETED_FILE_MATCHER2.processServerMessage(line);
		if (variables != null) {
			listener.deletedFile(
					(String)variables.get(REMOTE_FILE_PATH_VARIABLE_NAME),
					(String)variables.get(LEFT_REVISION_VARIABLE_NAME));
			return OK;
		}
		
		return super.messageLine(line, location, commandRoot, monitor);
	}

	@Override
	public IStatus errorLine(
		String line,
		ICVSRepositoryLocation location,
		ICVSFolder commandRoot,
		IProgressMonitor monitor) {
		
		Map variables = DIRECTORY_MATCHER.processServerMessage(line);
		if (variables != null) {
			listener.directory(
					(String)variables.get(REMOTE_FOLDER_PATH_VARIABLE_NAME));
			return OK;
		}
			
		return super.errorLine(line, location, commandRoot, monitor);
	}

}
