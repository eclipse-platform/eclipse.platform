/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

 
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This interface provides access to the specific portions of
 * the repository location string for use by connection methods
 * and the user authenticator.
 * 
 * @see IUserAuthenticator
 * @see IConnectionMethod
 * 
 * @noimplement It is not intended to implemented by clients.
 */
public interface ICVSRepositoryLocation  extends IAdaptable {

	/**
	 * port value which indicates to a connection method to use the default port
	 */
	public static int USE_DEFAULT_PORT = 0;
	
	/**
	 * Return the connection method for making the connection
	 */
	public IConnectionMethod getMethod();
	
	/**
	 * Returns the host where the repository is located
	 */
	public String getHost();
	
	/**
	 * Returns the port to connect to or USE_DEFAULT_PORT if
	 * the connection method is to use its default port.
	 */
	public int getPort();
	
	/**
	 * Returns the root directory of the repository.
	 */
	public String getRootDirectory();
	
	/**
	 * Returns the string representing the receiver. This string
	 * should contain enough information to recreate the receiver.
	 */
	public String getLocation(boolean forDisplay);

	/**
	 * Returns the immediate children of this location. If tag is <code>null</code> the
	 * HEAD branch is assumed.
	 * 
	 * If modules is true, then the module definitions from the CVSROOT/modules file are returned.
	 * Otherwise, the root level projects are returned.
	 * 
	 * @param tag the context in which to return the members (e.g. branch or version).
	 */
	public ICVSRemoteResource[] members(CVSTag tag, boolean modules, IProgressMonitor progress)  throws CVSException;
	
	/**
	 * Returns a handle to a remote file at this repository location using the given tag as the
	 * context. The corresponding remote file may not exist or may be a folder.
	 */
	public ICVSRemoteFile getRemoteFile(String remotePath, CVSTag tag);
	
	/**
	 * Returns a handle to a remote folder at this repository location using the given tag as the
	 * context. The corresponding remote folder may not exist or may be a file.
	 */
	public ICVSRemoteFolder getRemoteFolder(String remotePath, CVSTag tag);
	
	/**
	 * encoding for commit comments. 
	 */
	public String getEncoding();
		
	/**
	 * Return the connection timeout value in seconds.
	 * A value of 0 means there is no timeout value.
	 */
	public int getTimeout();
	
	/**
	 * Return the username 
	 */
	public String getUsername();
	
	/**
	 * Returns the user information for the location.
	 */
	public IUserInfo getUserInfo(boolean allowModificationOfUsername);	
	
	/**
	 * Flush any cached user information related to the repository location
	 */
	public void flushUserInfo();
	
	/**
	 * Validate that the receiver can be used to connect to a repository.
	 * An exception is thrown if connection fails
	 * 
	 * @param monitor the progress monitor used while validating
	 */
	public void validateConnection(IProgressMonitor monitor) throws CVSException;
	
	/**
	 * Set the option to allow the user settings to be cached between sessions.
	 * @since 3.0
	 */
	public void setAllowCaching(boolean allowCaching);

	/**
	 * Returns if the user info for this location is cached
	 */
	public boolean getUserInfoCached();
	
	/**
	 * Sets the user information used for this location
	 */
	public void setUsername(String username);
	
	/**
	 * Sets the user information used for this location
	 */
	public void setPassword(String password);
	
	/**
	 * Returns the plugged-in authenticator for this location.
	 * @since 3.0
	 */
	public IUserAuthenticator getUserAuthenticator();
	
	/**
	 * Sets the plugged-in authenticator for this location. This is a hook
	 * for testing.
	 * @since 3.0
	 */
	public void setUserAuthenticator(IUserAuthenticator authenticator);	

	/**
	 * Sets encoding for commit messages.
	 * @since 3.0
	 */
	public void setEncoding(String encoding);

}
