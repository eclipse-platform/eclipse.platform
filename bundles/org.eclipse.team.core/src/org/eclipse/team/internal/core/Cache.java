/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.*;
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

	Map properties;
	ListenerList listeners;

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizeOperationContext#addProperty(java.lang.String, java.lang.Object)
	 */
	public synchronized void put(String name, Object value) {
		if (properties == null) {
			properties = new HashMap();
		}
		properties.put(name, value);
	}

	public synchronized Object get(String name) {
		if (properties == null)
			return null;
		return properties.get(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizeOperationContext#removeProperty(java.lang.String)
	 */
	public synchronized void remove(String name) {
		if (properties != null)
			properties.remove(name);
		if (properties.isEmpty()) {
			properties = null;
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizeOperationContext#addDisposeListener(org.eclipse.team.ui.mapping.IDisposeListener)
	 */
	public synchronized void addCacheListener(ICacheListener listener) {
		if (listeners == null)
			listeners = new ListenerList(ListenerList.IDENTITY);
		listeners.add(listener);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizeOperationContext#removeDisposeListener(org.eclipse.team.ui.mapping.IDisposeListener)
	 */
	public synchronized void removeDisposeListener(ICacheListener listener) {
		removeCacheListener(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.ICache#removeCacheListener(org.eclipse.team.core.ICacheListener)
	 */
	public synchronized void removeCacheListener(ICacheListener listener) {
		if (listeners != null)
			listeners.remove(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizationContext#dispose()
	 */
	public void dispose() {
		if (listeners != null) {
			Object[] allListeners = listeners.getListeners();
			for (int i = 0; i < allListeners.length; i++) {
				final Object listener = allListeners[i];
				SafeRunner.run(new ISafeRunnable(){
					public void run() throws Exception {
						((ICacheListener)listener).cacheDisposed(Cache.this);
					}
					public void handleException(Throwable exception) {
						// Ignore since the platform logs the error

					}
				});
			}
		}
		properties = null;
	}

}
