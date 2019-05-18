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
package org.eclipse.team.internal.ccvs.core;

 
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;

 /**
 * This interface represents a file in a repository.
 * Instances of this interface can be used to fetch the contents
 * of the remote file.
 * 
 * In the future, additional information should be available (tags, revisions, etc.)
 * 
 * Clients are not expected to implement this interface.
 */
public interface ICVSRemoteFile extends ICVSRemoteResource, ICVSFile {

	/**
	 * Returns a stream over the contents of this remote element.
	 * 
	 * @param progress a progress monitor to indicate the duration of the operation, or
	 * <code>null</code> if progress reporting is not required.
	 */
	public InputStream getContents(IProgressMonitor progress) throws TeamException;
	
	/**
	 * Get the log entry for the revision the remote file represents.
	 * This method will return null until after the getContents(IProgressMonitor)
	 * method is called (i.e. the call to getContents also fetches the entry.
	 */
	public ILogEntry getLogEntry(IProgressMonitor monitor) throws TeamException;
	
	/**
	 * Get all the log entries of the remote file
	 */
	public ILogEntry[] getLogEntries(IProgressMonitor monitor) throws TeamException;
	
	/**
	 * Get the revision of the remote file (e.g. 1.1)
	 * 
	 * The revision depends on any tagging associated with the remote parent used
	 * to access the file. 
	 */
	public String getRevision() throws TeamException;
}

