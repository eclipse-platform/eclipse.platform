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
package org.eclipse.team.core;


/**
 * Listener that, when registered with a cache, gets invoked
 * when the cache is disposed.
 * <p>
 * Clients may implement this interface.
 *
 * @since 3.2
 */
public interface ICacheListener {

	/**
	 * The given cache has been disposed.
	 * @param cache the cache that was disposed
	 */
	void cacheDisposed(ICache cache);
}
