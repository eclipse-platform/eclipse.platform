/*******************************************************************************
 * Copyright (c) 2008, 2010 Oakland Software Incorporated and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *		Oakland Software Incorporated - initial API and implementation
 * 		IBM Corporation - implementation
 *******************************************************************************/
package org.eclipse.core.internal.net;

import java.net.URI;

import org.eclipse.core.net.proxy.IProxyData;

/**
 * Returns proxies to use.
 */
public abstract class AbstractProxyProvider {

	/**
	 * Returns proxies to use with the given URI. Returns empty array
	 * when there is no appropriate proxy.
	 *
	 * @param uri
	 *            the URI that a connection is required to
	 * @return an array of proxies for the given URI
	 */
	public abstract IProxyData[] select(URI uri);

	protected abstract IProxyData[] getProxyData();

	protected abstract String[] getNonProxiedHosts();
}
