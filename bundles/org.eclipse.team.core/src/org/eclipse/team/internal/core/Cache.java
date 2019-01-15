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
package org.eclipse.team.internal.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.team.core.ICache;
import org.eclipse.team.core.ICacheListener;

/**
 * A synchronize operation context that supports caching of
 * properties relevant to the operation and the registering of
 * dispose listeners.
 *
 * @see org.eclipse.team.core.ICache
 * @since 3.2
 */
public class Cache implements ICache {

	Map<String, Object> properties;
	ListenerList<ICacheListener> listeners;

	@Override
	public synchronized void put(String name, Object value) {
		if (properties == null) {
			properties = new HashMap<>();
		}
		properties.put(name, value);
	}

	@Override
	public synchronized Object get(String name) {
		if (properties == null)
			return null;
		return properties.get(name);
	}

	@Override
	public synchronized void remove(String name) {
		if (properties != null)
			properties.remove(name);
		if (properties.isEmpty()) {
			properties = null;
		}

	}

	@Override
	public synchronized void addCacheListener(ICacheListener listener) {
		if (listeners == null)
			listeners = new ListenerList<>(ListenerList.IDENTITY);
		listeners.add(listener);

	}

	@Override
	public synchronized void removeDisposeListener(ICacheListener listener) {
		removeCacheListener(listener);
	}

	@Override
	public synchronized void removeCacheListener(ICacheListener listener) {
		if (listeners != null)
			listeners.remove(listener);
	}

	public void dispose() {
		if (listeners != null) {
			for (ICacheListener listener : listeners) {
				SafeRunner.run(new ISafeRunnable(){
					@Override
					public void run() throws Exception {
						listener.cacheDisposed(Cache.this);
					}
					@Override
					public void handleException(Throwable exception) {
						// Ignore since the platform logs the error

					}
				});
			}
		}
		properties = null;
	}

}
