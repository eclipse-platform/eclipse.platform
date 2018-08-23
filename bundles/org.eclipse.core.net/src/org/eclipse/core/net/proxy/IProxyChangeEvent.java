/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
 * Event which describes a change in the proxy information managed by
 * the {@link IProxyService}.
 * <p>
 * This interface is not intended to be implemented by clients.
 * @see IProxyService
 * @since 1.0
 */
public interface IProxyChangeEvent {

	/**
	 * Type constant that indicates that the list of non-proxied hosts has changed.\
	 * @see #getChangeType()
	 */
	public static final int NONPROXIED_HOSTS_CHANGED = 1;

	/**
	 * Type constant that indicates that the data for one or more proxies has changed
	 * @see #getChangeType()
	 */
	public static final int PROXY_DATA_CHANGED = 2;

	/**
	 * Type constant that indicates that the enablement of the proxy
	 * service has changed. Client should consult the service to determine
	 * the current enablement
	 * @see #getChangeType()
	 * @see IProxyService#isProxiesEnabled()
	 */
	public static final int PROXY_SERVICE_ENABLEMENT_CHANGE = 3;

	/**
	 * Return the type of change this event represents. Clients
	 * should ignore types they do not recognize.
	 * @return the type of change this event represents
	 * @see #NONPROXIED_HOSTS_CHANGED
	 * @see #PROXY_DATA_CHANGED
	 */
	public int getChangeType();

	/**
	 * For a change type of {@link #NONPROXIED_HOSTS_CHANGED}, this method will
	 * return the list of non-proxied hosts before the change occurred.
	 * @return the list of non-proxied hosts before the change occurred
	 */
	public String[] getOldNonProxiedHosts();

	/**
	 * For a change type of {@link #NONPROXIED_HOSTS_CHANGED}, this method will
	 * return the list of non-proxied hosts after the change occurred.
	 * @return the list of non-proxied hosts after the change occurred
	 */
	public String[] getNonProxiedHosts();

	/**
	 * For a change type of {@link #PROXY_DATA_CHANGED}, this method returns
	 * the state of all known proxies before the change occurred.
	 * @return the state of all known proxies before the change occurred
	 */
	public IProxyData[] getOldProxyData();

	/**
	 * For a change type of {@link #PROXY_DATA_CHANGED}, this method returns
	 * the state of the changed known proxies after the change occurred. Clients
	 * should check the {@link IProxyService#isProxiesEnabled()} method to see
	 * if the proxy data change was the result of proxies being disabled.
	 * @return the state of the changed known proxies after the change occurred
	 */
	public IProxyData[] getChangedProxyData();

}
