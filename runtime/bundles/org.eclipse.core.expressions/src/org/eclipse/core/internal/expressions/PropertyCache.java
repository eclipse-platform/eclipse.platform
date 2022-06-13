/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.core.internal.expressions;

import org.eclipse.core.internal.expressions.util.LRUCache;

/* package */ class PropertyCache {

	private LRUCache fCache;

	public PropertyCache(final int cacheSize) {
		fCache= new LRUCache(100);
		fCache.setSpaceLimit(cacheSize);
	}

	public Property get(Property key) {
		return (Property)fCache.get(key);
	}

	public void put(Property method) {
		fCache.put(method, method);
	}

	public void remove(Property method) {
		fCache.removeKey(method);
	}
}
