/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

import java.net.URI;

import org.eclipse.core.runtime.CoreException;

/**
 * Manages the proxy data and related information. The proxy service is
 * registered as an OSGi service. Clients can obtain an instance of the service
 * from their bundle context or from a service tracker.
 *
 * <p>
 * Clients that wish to make a connection and need to determine whether to use a
 * proxy or not should call either {@link #getProxyDataForHost(String)} or
 * {@link #getProxyDataForHost(String, String)}.
 * </p>
 *
 * @since 1.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IProxyService {

	/**
	 * Sets whether proxy support should be enabled. The current proxy settings
	 * are still kept so clients should check the enablement using
	 * {@link #isProxiesEnabled()} before calling the {@link #getProxyData()} or
	 * {@link #getProxyData(String)} method. However, the
	 * {@link #getProxyDataForHost(String)} and
	 * {@link #getProxyDataForHost(String, String)} method will check the
	 * enablement and only return data if the service is enabled.
	 *
	 * @param enabled
	 *            whether proxy support should be enabled
	 */
	void setProxiesEnabled(boolean enabled);

	/**
	 * Returns whether proxy support should be enabled. When disabled, all
	 * connections will be direct.
	 *
	 * <p>
	 * Returns <code>false</code>, when the system proxies support is
	 * enabled but {@link #hasSystemProxies()} returns <code>false</code>.
	 * </p>
	 *
	 * @return whether proxy support should be enabled
	 */
	boolean isProxiesEnabled();

	/**
	 * Returns whether system proxy support is available.
	 *
	 * @return whether system proxy support is available
	 * @since 1.1
	 */
	boolean hasSystemProxies();

	/**
	 * Sets whether system proxies should be used, when the proxy support is
	 * enabled.
	 *
	 * @param enabled whether system proxies should be used
	 * @since 1.1
	 */
	void setSystemProxiesEnabled(boolean enabled);


	/**
	 * Returns whether system proxy should be used when the proxy support is
	 * enabled.
	 *
	 * @return whether system proxy is used when the proxy support is enabled
	 * @since 1.1
	 */
	boolean isSystemProxiesEnabled();

	/**
	 * Returns the list of know proxy types and their settings. Some of the
	 * returned proxy types may not be enabled (i.e, their hosts may be
	 * <code>null</code>.
	 *
	 * <p>
	 * Clients that wish to make a connection and need to determine whether to
	 * use a proxy or not should call either
	 * {@link #getProxyDataForHost(String)} or
	 * {@link #getProxyDataForHost(String, String)}.
	 * </p>
	 *
	 * <p>
	 * This method returns the proxies set via {@link #setProxyData(IProxyData[])}
	 * </p>
	 *
	 * @return the list of know proxy types and their settings
	 */
	IProxyData[] getProxyData();

	/**
	 * Returns all the applicable proxy data to access the specified URI.
	 * <p>
	 * Clients that wish to make a connection and need to determine whether to
	 * use a proxy should use this method.
	 * </p>
	 * @param uri
	 *            the URI for which proxies are returned
	 * @return list of all applicable proxy data, if no proxy is applicable then
	 *         an empty array is returned
	 *
	 * @since 1.2
	 */
	IProxyData[] select(URI uri);

	/**
	 * Returns the list of known proxy types and their settings for the
	 * given host. If proxies are disabled
	 * or if the host matches any entries in the non-proxied
	 * hosts lists or if a matching proxy type has no data, then
	 * an empty array is returned.
	 *
	 * <p>
	 * Clients that wish to make a connection and need to determine whether to
	 * use a proxy should use this method.
	 * </p>
	 * @deprecated This method is deprecated because of its ambiguity. Use
	 * {@link #select(URI)} instead.
	 *
	 * @param host the host for which a connection is desired
	 * @return the list of known proxy types and their settings for the
	 * given host
	 */
	@Deprecated
	IProxyData[] getProxyDataForHost(String host);

	/**
	 * Returns the proxy data for the proxy of the given type or
	 * <code>null</code> if the proxy type is not known by this service.
	 *
	 * <p>
	 * Clients that wish to make a connection and need to determine whether to
	 * use a proxy or not should call either
	 * {@link #getProxyDataForHost(String)} or
	 * {@link #getProxyDataForHost(String, String)}.
	 * </p>
	 *
	 * <p>
	 * This method returns the proxies set via {@link #setProxyData(IProxyData[])}
	 * </p>
	 *
	 * @param type
	 *            the proxy type
	 * @return the proxy data for the proxy of the given type or
	 *         <code>null</code>
	 * @see IProxyData#HTTP_PROXY_TYPE
	 * @see IProxyData#HTTPS_PROXY_TYPE
	 * @see IProxyData#SOCKS_PROXY_TYPE
	 */
	IProxyData getProxyData(String type);

	/**
	 * Returns the proxy data for the proxy of the given type or
	 * <code>null</code> if the proxy type is not known by this service, the
	 * proxy data is empty for that type or the host is in the non-proxied host
	 * list.
	 *
	 * <p>
	 * Clients that wish to make a connection and need to determine whether to
	 * use a proxy should use this method.
	 * </p>
	 *  @deprecated This method is deprecated because of its ambiguity. Use
	 * {@link #select(URI)} instead.
	 *
	 * @param host
	 *            the host for which a connection is desired
	 * @param type
	 *            the proxy type
	 * @return the proxy data for the proxy of the given type or
	 *         <code>null</code>
	 * @see IProxyData#HTTP_PROXY_TYPE
	 * @see IProxyData#HTTPS_PROXY_TYPE
	 * @see IProxyData#SOCKS_PROXY_TYPE
	 */
	@Deprecated
	IProxyData getProxyDataForHost(String host, String type);

	/**
	 * Sets the information associated with known proxy types. If an unknown
	 * type is provided, it will be ignored. Any known types that are not
	 * present in the list of the provided proxy data will be unaffected.
	 *
	 * <p>
	 * This method affects only proxies defined in Eclipse by user and doesn't
	 * affect settings of the system proxies (being used when the system support
	 * is enabled).
	 * </p>
	 *
	 * @param proxies
	 *            the proxy data whose information is to be set.
	 * @throws CoreException
	 *             if the proxy could not be set
	 */
	void setProxyData(IProxyData[] proxies) throws CoreException;

	/**
	 * Returns the list of hosts for which non proxy should be used. The values
	 * returned from this method should only be used for displaying the
	 * non-proxed hosts list.
	 *
	 * <p>
	 * Clients that wish to make a connection and need to determine whether to
	 * use a proxy or not should call either
	 * {@link #getProxyDataForHost(String)} or
	 * {@link #getProxyDataForHost(String, String)}.
	 * </p>
	 *
	 * <p>
	 * This method returns the hosts set via
	 * {@link #setNonProxiedHosts(String[])}
	 * </p>
	 *
	 * @return the list of hosts for which non proxy should be used.
	 * @see #getProxyDataForHost(String)
	 * @see #getProxyDataForHost(String, String)
	 */
	String[] getNonProxiedHosts();

	/**
	 * Sets the list of hosts for which non proxy should be used.
	 *
	 * <p>
	 * This method affects only non-proxied hosts defined in Eclipse by user and
	 * doesn't affect settings of the system proxies (being used when the system
	 * support is enabled).
	 * </p>
	 *
	 * @param hosts
	 *            the list of hosts for which non proxy should be used
	 * @throws CoreException
	 *             if the non-proxied host list could not be set
	 */
	void setNonProxiedHosts(String[] hosts) throws CoreException;

	/**
	 * Registers a listener that will be notified when proxy related
	 * information changes. Adding a listener that is already registered
	 * has no effect.
	 * @param listener a change listener
	 */
	void addProxyChangeListener(IProxyChangeListener listener);

	/**
	 * Removes a listener. Removing a listener that is not registered
	 * has no effect.
	 * @param listener a change listener
	 */
	void removeProxyChangeListener(IProxyChangeListener listener);
}
