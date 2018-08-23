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
 *     Olexiy Buyanskyy <olexiyb@gmail.com> - Bug 76386 - [History View] CVS Resource History shows revisions from all branches
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core;


import java.util.Date;

import org.eclipse.core.runtime.IAdaptable;

/**
 * Instances of ILogEntry represent an entry for a CVS file that results
 * from the cvs log command.
 * 
 * Clients are not expected to implement this interface
 */
public interface ILogEntry extends IAdaptable {

	/**
	 * Get the revision for the entry
	 */
	public String getRevision();
	
	/**
	 * Get the author of the revision
	 */
	public String getAuthor();
	
	/**
	 * Get the date the revision was committed
	 */
	public Date getDate();
	
	/**
	 * Get the comment for the revision
	 */
	public String getComment();
	
	/**
	 * Get the state
	 */
	public String getState();
	
	/**
	 * Get the branches revision belong to.
	 */
	public CVSTag[] getBranches();

	/**
	 * Get the tags associated with the revision
	 */
	public CVSTag[] getTags();
	
	/**
	 * Get the remote file for this entry
	 */
	public ICVSRemoteFile getRemoteFile();
	
	/**
	 * Does the log entry represent a deletion (stat = "dead")
	 */
	public boolean isDeletion();
}

