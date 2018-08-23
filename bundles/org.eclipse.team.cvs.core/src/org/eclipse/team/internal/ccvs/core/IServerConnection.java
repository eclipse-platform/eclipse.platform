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
 
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.connection.CVSAuthenticationException;
/**
* CVS supports different connection methods for communicating between a client and the server.
* Furthermore, custom connection methods can be added. Connection methods are added
* to the CVS client as an IConnectionMethod, which can be used to create connections of 
* type IServerConnection.
* 
* @see IConnectionMethod
*/
public interface IServerConnection {
	/**
	 * Open a connection to the CVS server.
	 * 
	 * Throw CVSAuthenticationException if the username or password is invalid.
	 * Throw IOExceptions for other failures.
	 */
	public void open(IProgressMonitor monitor) throws IOException, CVSAuthenticationException;
	/**
	 * Close the connection
	 * 
	 * Throw IOException on failures
	 */
	public void close() throws IOException;
	/**
	 * Get the input stream to receive responses from the server
	 */
	public InputStream getInputStream();
	/**
	 * Get the output stream to send requests to the server
	 */
	public OutputStream getOutputStream();
}
