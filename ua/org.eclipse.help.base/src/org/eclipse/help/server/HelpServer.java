/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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

package org.eclipse.help.server;
import org.eclipse.core.runtime.CoreException;

/**
 * @since 3.4
 * Abstract class representing a web server which can be used to host the Eclipse help
 * system using the extension point org.eclipse.help.base.server. Classes extending this
 * abstract class must be capable of launching a Web Server and
 */

public abstract class HelpServer {

	/**
	 * Start a server application to host the Eclipse help system. The server is
	 * responsible for initializing the servlets, jsp files and other resources for
	 * the help system as defined by the extension points
	 * <code>org.eclipse.equinox.http.registry.resources</code> and
	 * <code>org.eclipse.equinox.http.registry.servlets</code> for the httpcontextId
	 * <code>org.eclipse.help.webapp.help</code>
	 *
	 * @param webappName The name of this web application
	 * @throws Exception If anything prevented starting the server.
	 */
	public abstract void start(String webappName) throws Exception;

	/**
	 * Stop a server application. If an application of this name has not been
	 * started do nothing
	 *
	 * @param webappName the name of a running web application
	 * @throws CoreException If there was any error during stopping.
	 */
	public abstract void stop(String webappName) throws CoreException ;

	/**
	* Returns the port number the app server listens on
	* @return integer port number, 0 if server not started
	*/
	public abstract int getPort();


	/**
	* Returns the host name or ip the app server runs on.
	*
	* @return String representation of host name of IP, null if server not
	*         started yet
	*/
	public abstract String getHost();
}
