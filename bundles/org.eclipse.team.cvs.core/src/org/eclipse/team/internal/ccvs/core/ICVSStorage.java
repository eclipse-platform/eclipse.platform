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

/**
 * This interface is used by the Session to transfer file contents.
 * It is used for regular files in a local sandbox as well as special files
 * such as the CVS/Template file.
 */
public interface ICVSStorage {

	/**
	 * Return the name for this ICVSStorage.
	 * @return 
	 */
	String getName();
	
	/**
	 * Set the contents of the file to the contents of the provided input stream.
	 * 
	 * This method is used by the command framework and should not be used by other clients.
	 * Other clients should set the contents of the underlying <code>IFile</code> which
	 * can be obtained using <code>getIResource()</code>.
	 * 
	 * @param responseType the type of reponse that was received from the server
	 * 
	 *    UPDATED - could be a new file or an existing file
	 *    MERGED - merging remote changes with local changes. Failure could result in loss of local changes
	 *    CREATED - contents for a file that doesn't exist locally
	 *    UPDATE_EXISTING - Replacing a local file with no local changes with remote changes.
	 */
	public void setContents(InputStream stream, int responseType, boolean keepLocalHistory, IProgressMonitor monitor) throws CVSException;

	/**
	 * Answers the size of the file. 
	 */
	long getSize();
	
	/**
	 * Gets an input stream for reading from the file.
	 * It is the responsibility of the caller to close the stream when finished.
	 */
	InputStream getContents() throws CVSException;
	
}
