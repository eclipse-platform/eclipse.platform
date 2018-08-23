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


import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * The CVS analog of file system files and directories. These are handles to
 * state maintained by a CVS client. That is, the CVS resource does not 
 * actually contain data but rather represents CVS state and behavior. You are
 * free to manipulate handles for CVS resources that do not exist but be aware
 * that some methods require that an actual resource be available.
 * <p>
 * The CVS client has been designed to work on these handles uniquely. As such, the
 * handle could be to a remote resource or a local resource and the client could
 * perform CVS operations ignoring the actual location of the resources.</p>
 * 
 * @see ICVSFolder
 * @see ICVSFile
 */
public interface ICVSResource {
	
	/**
	 * Answers the name of the resource.
	 * 
	 * @return the name of the resource this handle represents. It can never
	 * be <code>null</code>.
	 */
	public String getName();
	
	/**
	 * Answers if this resource has CVS synchronization information associated
	 * with it.
	 * 
	 * @return <code>true</code> if the resource is
	 */
	public boolean isManaged() throws CVSException;

	/**
	 * Unmanage the given resource by purging any CVS synchronization associated with the 
	 * resource. The only way a resource can become managed is by running the 
	 * appropriate CVS commands (e.g. add/commit/update).
	 */
	public void unmanage(IProgressMonitor monitor) throws CVSException;

	/**
	 * Answer whether the resource could be ignored because it is in the one of the 
	 * ignore lists maintained by CVS. Even if a resource is ignored, it can still be
	 * added to a repository, at which time it should never be ignored by the CVS
	 * client.
	 * 
	 * @return <code>true</code> if this resource is listed in one of the ignore
	 * files maintained by CVS and <code>false</code> otherwise.
	 */
	public boolean isIgnored() throws CVSException;
	
	/**
	 * Add the following pattern to the file's parent ignore list
	 * 
	 * XXX This should really be a method of ICVSFolder
	 */
	public void setIgnoredAs(String pattern) throws CVSException;
			
	/**
	 * Answers if the handle is a file or a folder handle.
	 * 
	 * @return <code>true</code> if this is a folder handle and <code>false</code> if
	 * it is a file handle.
	 */
	public boolean isFolder();
	
	/**
	 * Answers if the resource identified by this handle exists.
	 * 
	 * @return <code>true</code> if the resource represented by this handle
	 * exists and <code>false</code> false otherwise.
	 */
	public boolean exists() throws CVSException;	

	/**
	 * Answers the underlying IResource for the cvs resource (or null if there
	 * is not a corresponding local resource).
	 * 
	 * @return the IResource that corresponds to the CVS resource
	 */
	public IResource getIResource();
	
	/**
	 * Answers the local relative path from the given ancestor to the receiver.
	 * This method will return a path for files that are themselves not added
	 * to CVS control but who have an ancestor that is under CVS control.
	 * 
	 * @return the ancestor relative path for this resource.
	 */
	public String getRelativePath(ICVSFolder ancestor) throws CVSException;

	/**
	 * Return the repository relative path of the remote resource. Return
	 * <code>null</code> if the resource is not under CVS control.
	 * 
	 * @return
	 * @throws CVSException
	 */
	public String getRepositoryRelativePath() throws CVSException;
	
	/**
	 * Get the absolute remote location of a resource. This method is used by
	 * the CVS command infrastructure during command execution. The root is used
	 * in situations where the resource is not under CVS control. The remote
	 * path that the resource would have if it was is determined by recursively
	 * searching the resource's parent until a managed folder is found. The
	 * provided root is used to stop the recursive search if no managed parent
	 * is found.
	 * 
	 * @param root the root folder of the command.
	 * 
	 * @return the remote location.
	 */
	public String getRemoteLocation(ICVSFolder root) throws CVSException;
	
	/**
	 * Answers the workspace synchronization information for this resource. This would 
	 * typically include information from the <b>Entries</b> file that is used to track
	 * the base revisions of local CVS resources.
	 * 
	 * @return the synchronization information for this resource, or <code>null</code>
	 * if the resource does not have synchronization information available.
	 */
	public ResourceSyncInfo getSyncInfo() throws CVSException;

	/** 
	 * Deletes the resource represented by the handle.
	 */
	public void delete() throws CVSException;
	
	/**
	 * Give the folder that contains this resource. If the resource is not managed 
	 * then the result of the operation is not specified.
	 * 
	 * @return a handle to the parent of this resource.
	 */
	public ICVSFolder getParent();

	/**
	 * Accept a vistor to this resource.
	 */
	public void accept(ICVSResourceVisitor visitor) throws CVSException;
	
	/**
	 * Accept a visitor to this resource. The recurse parameter corresponds to the CVS
	 * -l (do not recurse) and -R (recurse) options. If recurse is false, only the resource
	 * and it's children are visited. Otherwise, the resource and all it's decendants are
	 * visited.
	 */
	public void accept(ICVSResourceVisitor visitor, boolean recurse) throws CVSException;
	
	/**
	 * Method isModified.
	 * @return boolean
	 */
	public boolean isModified(IProgressMonitor monitor) throws CVSException;
}
