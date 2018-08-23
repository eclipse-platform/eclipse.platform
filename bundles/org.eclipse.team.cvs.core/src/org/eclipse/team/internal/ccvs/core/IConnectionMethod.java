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

 
/**
 * Implementators of this class can act as factories for creating connections to a CVS server
 * with the desired custom communication protocol. Providers of CVS connection methods must implement 
 * this interface and register the implementation with the extension point:
 * 
 * 		org.eclipse.team.cvs.core.connectionmethods
 * 
 * The <code>createConnection()</code> method will be invoked by the CVS client when the user 
 * is attempting to make a connection to the server using the connection name which matches
 * the <code>String</code> returned by <code>getName()</code> (e.g. "pserver", "ext", etc.).
 */
public interface IConnectionMethod {
	
	/**
	 * Returns the name of this connection method (e.g."local", "ext").
	 */
	public String getName();
	
	/**
	 * Creates a new server connection using the given repository root
	 * (which includes the user name) and the given password.
	 * If password is not given, null will be passed.
	 */
	public IServerConnection createConnection(ICVSRepositoryLocation location, String password);
	
	/**
	 * Some connection method may persist the physical connection to the server
	 * through several IServerConnections.  For example, when making several
	 * successive connections to the same location using SSH2, it would be very
	 * expensive to re-connect, re-negotiate and re-authenticate for each
	 * operation; therefore the SSH2 connection method will create one SSH
	 * session and open several channels (one for each IServerConnection
	 * created), and keep the session open until disconnect() is called.
	 * <p>
	 *  This method actually closes any connection to the indicated location.
	 * </p>
	 */
	public void disconnect(ICVSRepositoryLocation location);
}
