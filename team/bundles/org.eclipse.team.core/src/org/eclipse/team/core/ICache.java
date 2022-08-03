/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 * A cache that is associated with a synchronization that allows clients
 * to cache synchronization state related to their model for the duration of the
 * operation. When the context is disposed, the cache will be disposed and any
 * listeners notified.
 *
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ICache {

	/**
	 * Cache the given object with this context.
	 * @param name the name that uniquely identifies the object
	 * @param value the value to be cached.
	 */
	void put(String name, Object value);

	/**
	 * Retrieve an object that has been cached with the context
	 * @param name the name of the object
	 * @return the object associated with the name or <code>null</code>
	 */
	Object get(String name);

	/**
	 * Remove the named object from the cache
	 * @param name the name
	 */
	void remove(String name);

	/**
	 * Add a listener to the cache that will receive notification
	 * when the cache is disposed. Adding a listener that has already
	 * been added has no effect.
	 * @param listener the listener to add
	 */
	void addCacheListener(ICacheListener listener);

	/**
	 * Remove the listener. Removing a listener that is not registered
	 * has no effect.
	 * @param listener the listener to remove
	 * @since 3.3
	 */
	void removeCacheListener(ICacheListener listener);

	/**
	 * Remove the listener. Removing a listener that is not registered
	 * has no effect.
	 * @param listener the listener to remove
	 * @deprecated use {@link #removeCacheListener(ICacheListener)}
	 */
	@Deprecated
	void removeDisposeListener(ICacheListener listener);

}
