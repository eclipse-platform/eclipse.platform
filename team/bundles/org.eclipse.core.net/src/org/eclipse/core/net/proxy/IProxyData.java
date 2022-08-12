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
 *******************************************************************************/
package org.eclipse.core.net.proxy;

/**
 * An {@link IProxyData} contains the information that is required to connect to
 * a particular proxy server.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.0
 */
public interface IProxyData {

	/**
	 * Type constant (value "HTTP") which identifies an HTTP proxy.
	 * @see #getType()
	 */
	public static final String HTTP_PROXY_TYPE = "HTTP"; //$NON-NLS-1$

	/**
	 * Type constant (value "HTTPS") which identifies an HTTPS proxy.
	 * @see #getType()
	 */
	public static final String HTTPS_PROXY_TYPE = "HTTPS"; //$NON-NLS-1$

	/**
	 * Type constant (value "SOCKS") which identifies an SOCKS proxy.
	 * @see #getType()
	 */
	public static final String SOCKS_PROXY_TYPE = "SOCKS"; //$NON-NLS-1$

	/**
	 * Return the type of this proxy. Additional proxy types may be
	 * added in the future so clients should accommodate this.
	 * @return the type of this proxy
	 * @see #HTTP_PROXY_TYPE
	 * @see #HTTPS_PROXY_TYPE
	 * @see #SOCKS_PROXY_TYPE
	 */
	String getType();

	/**
	 * Return the host name for the proxy server or <code>null</code>
	 * if a proxy server of this type is not available.
	 * @return the host name for the proxy server or <code>null</code>
	 */
	String getHost();

	/**
	 * Set the host name for the proxy server of this type.
	 * If no proxy server of this type is available, the host name should
	 * be set to <code>null</code>.
	 * <p>
	 * Setting this value will not affect the data returned from {@link IProxyService#getProxyData()}.
	 * Clients can change the global settings by changing the proxy data instances and then
	 * by calling {@link IProxyService#setProxyData(IProxyData[])} with the adjusted data.
	 * @param host the host name for the proxy server or <code>null</code>
	 */
	void setHost(String host);

	/**
	 * Return the port that should be used when connecting to the host or -1
	 * if the default port for the proxy protocol should be used.
	 * @return the port that should be used when connecting to the host
	 */
	int getPort();

	/**
	 * Set the port that should be used when connecting to the host. Setting the port
	 * to a value of -1 will indicate that the default port number for
	 * the proxy type should be used.
	 * <p>
	 * Setting this value will not affect the data returned from {@link IProxyService#getProxyData()}.
	 * Clients can change the global settings by changing the proxy data instances and then
	 * by calling {@link IProxyService#setProxyData(IProxyData[])} with the adjusted data.
	 * @param port the port that should be used when connecting to the host
	 * 	or -1 if the default port is to be used
	 */
	void setPort(int port);

	/**
	 * Return the id of the user that should be used when authenticating
	 * for the proxy. A <code>null</code> is returned if there is no
	 * authentication information.
	 * @return the id of the user that should be used when authenticating
	 * for the proxy or <code>null</code>
	 */
	String getUserId();

	/**
	 * Set the id of the user that should be used when authenticating
	 * for the proxy. A <code>null</code> should be used if there is no
	 * authentication information.
	 * <p>
	 * Setting this value will not affect the data returned from {@link IProxyService#getProxyData()}.
	 * Clients can change the global settings by changing the proxy data instances and then
	 * by calling {@link IProxyService#setProxyData(IProxyData[])} with the adjusted data.
	 * @param userid the id of the user that should be used when authenticating
	 * for the proxy or <code>null</code>
	 */
	void setUserid(String userid);

	/**
	 * Return the password that should be used when authenticating
	 * for the proxy. A <code>null</code> is returned if there is no
	 * password or the password is not known.
	 * @return the password that should be used when authenticating
	 * for the proxy or <code>null</code>
	 */
	String getPassword();

	/**
	 * Set the password that should be used when authenticating
	 * for the proxy. A <code>null</code> should be passed if there is no
	 * password or the password is not known.
	 * <p>
	 * Setting this value will not affect the data returned from {@link IProxyService#getProxyData()}.
	 * Clients can change the global settings by changing the proxy data instances and then
	 * by calling {@link IProxyService#setProxyData(IProxyData[])} with the adjusted data.
	 * @param password the password that should be used when authenticating
	 * for the proxy or <code>null</code>
	 */
	void setPassword(String password);

	/**
	 * Returns whether the proxy requires authentication. If the proxy
	 * requires authentication but the user name and password fields of
	 * this proxy data are null, the client can expect the connection
	 * to fail unless they somehow obtain the authentication information.
	 * @return whether the proxy requires authentication
	 */
	boolean isRequiresAuthentication();

	/**
	 * Set the values of this data to represent a disabling of its type.
	 * Note that the proxy type will not be disabled unless the client
	 * calls {@link IProxyService#setProxyData(IProxyData[])} with the
	 * disabled data as a parameter. A proxy data can be enabled by setting
	 * the host.
	 */
	void disable();

}
