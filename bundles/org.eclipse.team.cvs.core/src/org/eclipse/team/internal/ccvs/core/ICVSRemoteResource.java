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

 
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;

/**
 * The interface represents a resource that exists in a CVS repository.
 * It purpose is to provide information about the remote resource from
 * the repository.
 * 
 * Clients are not expected to implement this interface.
 */
public interface ICVSRemoteResource extends ICVSResource, IAdaptable {
	
	/**
	 * Answers if the remote element may have children.
	 * 
	 * @return <code>true</code> if the remote element may have children and 
	 * <code>false</code> otherwise.
	 */
	public boolean isContainer();
	
	/**
	 * Return the repository
	 */
	public ICVSRepositoryLocation getRepository();
	
	/**
	 * Returns the parent of this remote resource or <code>null</code> if the
	 * remote resource does not have a parent.
	 */
	public ICVSRemoteResource getRemoteParent();
	
	/**
	 * Does the remote resource represented by this handle exist on the server. This
	 * method may contact the server and be long running.
	 */
	public boolean exists(IProgressMonitor monitor) throws TeamException;
	
	/**
	 * Answers the repository relative path of this remote folder.
	 */
	public String getRepositoryRelativePath();
	
	/**
	 * Compares two objects for equality; for cvs remote resources, equality is defined in 
	 * terms of their handles: same cvs resource type, equal relative paths, and
	 * for files, identical revision numbers. Remote resources are not equal to objects other 
	 * than cvs remote resources.
	 *
	 * @param other the other object
	 * @return an indication of whether the objects are equals
	 */
	public boolean equals(Object other);

	/**
	 * Allows a client to change the context of a remote resource handle.  For
	 * example, if a remote resource was created with the HEAD context (e.g. can
	 * be used to browse the main branch) use this method to change the
	 * context to another branch tag or to a version tag.
	 */
	public ICVSRemoteResource forTag(CVSTag tagName);
			
	/**
	 * Tag the remote resources referenced by the receiver (using rtag)
	 */
	public IStatus tag(CVSTag tag, LocalOption[] localOptions, IProgressMonitor monitor) throws CVSException;
	
	/**
	 * TODO: Temporary 
	 * @param progress
	 * @return
	 */
	public ICVSRemoteResource[] members(IProgressMonitor progress) throws TeamException;

}
