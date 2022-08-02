/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
 *     Atsuhiko Yamanaka, JCraft,Inc. - adding methods for using IJSchLocation
 *******************************************************************************/
package org.eclipse.jsch.core;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.runtime.IProgressMonitor;

import com.jcraft.jsch.*;

/**
 * A service whose purpose is to ensure that all the JSch preferences are properly pushed into JSch
 * before a {@link Session} is created. The service is registered as
 * an OSGi service. Clients can obtain an instance of the service from their bundle context
 * or from a service tracker.
 * 
 * This interface is not intended to be implemented by clients.
 * @since 1.0
 * @noimplement
 */
public interface IJSchService{

  /**
   * Create a {@link Session} that can be used to make SSH2 connections. This method ensures that 
   * all preferences are properly propagated into JSch before creating the session and also
   * ensures that the session uses the appropriate proxy settings.
   * <p>
   * Calling this method does not connect the session (see {@link #connect(Session, int, IProgressMonitor)}, if connection
   * throws an exception, clients should check to see if the session is still connected (see {@link Session#isConnected()}.
   * If it is, they should call {@link Session#disconnect()}.
   * 
   * @param host the host name
   * @param port the port or -1 if the default port is to be used
   * @param username the user name or <code>null</code> if there is no user name or the user name is not known
   
   * @return the created session
   * @throws JSchException if errors occur
   */
  public abstract Session createSession(String host, int port, String username)
      throws JSchException;

  /**
   * 
   * Create a {@link Session} that can be used to make SSH2 connections. This method ensures that 
   * all preferences are properly propagated into JSch before creating the session and also
   * ensures that the session uses the appropriate proxy settings.
   * <p>
   * Calling this method does not connect the session (see {@link #connect(Session, int, IProgressMonitor)}, if connection
   * throws an exception, clients should check to see if the session is still connected (see {@link Session#isConnected()}.
   * If it is, they should call {@link Session#disconnect()}.
   * 
   *
   * @param location the location which corresponds to user@host:port
   * @param uinfo an instance of {@link UserInfo} or <code>null</code> if
   *        the internal UserInfo implementation should be used.
   * @return the created session
   * @throws JSchException if errors occur
   * @since 1.1
   */
  public abstract Session createSession(IJSchLocation location, UserInfo uinfo)
  throws JSchException;

  /**
   * Connect the session using a responsive socket factory. The timeout value is used
   * for socket creation only. Clients that desire a timeout on the session must
   * call {@link Session#setTimeout(int)}. If session connection fails due to an exception,
   * the session will be disconnected by this method.
   * 
   * @param session the session to be connected
   * @param timeout
   *          a timeout in milliseconds
   * @param monitor
   *          a progress monitor or <code>null</code> if progress and
   *          cancelation is not desired
   * @throws JSchException if an exception occurs connecting the session.
   */
  public abstract void connect(Session session, int timeout,
      IProgressMonitor monitor) throws JSchException;

  /**
   * Return the proxy that should be used to connect to the given host or <code>null</code>
   * if no proxy is specified for the host.
   * @param host the host
   * @param proxyType the proxy type (either {@link IProxyData#HTTPS_PROXY_TYPE} or {@link IProxyData#SOCKS_PROXY_TYPE})
   * @return the proxy that should be used to connect to the given host or <code>null</code>
   * if no proxy is specified for the host
   */
  public abstract Proxy getProxyForHost(String host, String proxyType);
  
  /**
   * Connect to the given host and port using the given proxy.
   * This method calls {@link Proxy#connect(SocketFactory, String, int, int)}
   * and provides a {@link SocketFactory} that responds to cancelation.
   * @param proxy the proxy
   * @param host the host name
   * @param port the port
   * @param timeout
   *          a timeout in milliseconds
   * @param monitor
   *          a progress monitor or <code>null</code> if progress and
   *          cancelation is not desired
   * @throws JSchException if errors occur
   */
  public abstract void connect(Proxy proxy, String host, int port, int timeout,
      IProgressMonitor monitor) throws JSchException;

  /**
   * Get the IJSchLocation according to given user name, host name and port number.
   *
   * @param user user name for ssh2 connection
   * @param host host name for ssh2 connection
   * @param port port number for ssh2 connection
   * @return the created IJSchLocation
   * @since 1.1
   */
  public abstract IJSchLocation getLocation(String user, String host, int port);
  
  /**
   * Get the singleton instance of JSch allocated in jsch.core plug-in internally.
   * @return the singleton instance of JSch.
   * @since 1.1
   */
  public abstract JSch getJSch();
}