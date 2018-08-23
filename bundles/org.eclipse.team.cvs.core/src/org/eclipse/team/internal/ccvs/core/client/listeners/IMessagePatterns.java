/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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

import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * This class contains the default server message patterns
 */
public interface IMessagePatterns {

	public static final String SERVER_MESSAGE_PREFIX = "\\w* \\w*: "; //$NON-NLS-1$
	public static final String SERVER_ABORTED_MESSAGE_PREFIX = "\\w* [\\w* aborted]: "; //$NON-NLS-1$
	
	// TODO: These patterns could be more specific but this would require non-capturing
	// groups which currently throw off variable matching
	public static final String TAG_PATTERN = "[\\w\\-]*"; //$NON-NLS-1$
	public static final String REVISION_PATTERN = ".*"; //$NON-NLS-1$
	public static final String FILE_PATH_PATTERN = ".*"; //$NON-NLS-1$
	
	// TODO: It would be better if the prefix was optional but this requires the use of a capturing group which throws the group count off
	public static final String RDIFF_DIRECTORY = SERVER_MESSAGE_PREFIX + "Diffing " + Util.getVariablePattern(FILE_PATH_PATTERN, "remoteFolderPath"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final String RDIFF_SUMMARY_FILE_DIFF = "File " + Util.getVariablePattern(FILE_PATH_PATTERN, "remoteFilePath") + " changed from revision " + Util.getVariablePattern(REVISION_PATTERN, "leftRevision") + " to " + Util.getVariablePattern(REVISION_PATTERN, "rightRevision"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	public static final String RDIFF_SUMMARY_NEW_FILE = "File " + Util.getVariablePattern(FILE_PATH_PATTERN, "remoteFilePath") + " is new; " + TAG_PATTERN + " revision " + Util.getVariablePattern(REVISION_PATTERN, "rightRevision"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	public static final String RDIFF_SUMMARY_DELETED_FILE = "File " + Util.getVariablePattern(FILE_PATH_PATTERN, "remoteFilePath") + " is removed; not included in release tag " + TAG_PATTERN; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	// This format was introduced in 1.11.7
	public static final String RDIFF_SUMMARY_DELETED_FILE2 = "File " + Util.getVariablePattern(FILE_PATH_PATTERN, "remoteFilePath") + " is removed; " + TAG_PATTERN + " revision " + Util.getVariablePattern(REVISION_PATTERN, "leftRevision"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	
	public static final String MERGE_UPDATE_CONFLICTING_ADDITION = SERVER_MESSAGE_PREFIX + "file " + Util.getVariablePattern(FILE_PATH_PATTERN, "localFilePath") + " exists, but has been added in revision " + TAG_PATTERN;  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
}
