/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.variables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILogicalStructureType;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate3;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.internal.ui.DebugUIPlugin;

/**
 * Cache that stores evaluated logical structure values to replace raw values.  Cache
 * should be cleared when a RESUME or TERMINATE event is fired so the structure can be
 * reevaluated for new values.
 *
 * @since 3.3
 *
 */
public class LogicalStructureCache {

	/**
	 * Maps a ILogicalStructureType to the cache for that type
	 */
	private Map<ILogicalStructureType, LogicalStructureTypeCache> fCacheForType = new HashMap<>();

	/**
	 * Returns the logical value to replace the given value using the specified logical structure.
	 * The value will be retrieved from the cache if possible, or evaluated if not.
	 *
	 * @param type the logical structure type used to evaluate the logical value
	 * @param value the raw value to replace with a logical structure
	 * @return the logical value replacing the raw value or <code>null</code> if there is a problem
	 */
	public IValue getLogicalStructure(ILogicalStructureType type, IValue value) throws CoreException {
		synchronized (fCacheForType) {
			LogicalStructureTypeCache cache = getCacheForType(type);
			return cache.getLogicalStructure(value);
		}
	}

	/**
	 * Clears the cache of all evaluated values.
	 */
	public void clear(){
		Collection<LogicalStructureTypeCache> caches;
		synchronized (fCacheForType) {
			caches = new ArrayList<>(fCacheForType.values());
			fCacheForType.clear();
		}

		caches.forEach(LogicalStructureTypeCache::dispose);
	}

	/**
	 * Helper method that returns the cache associated with the given logical structure type.
	 * If there is not cache associated, one is created.
	 *
	 * @param type the logical structure type to get the cache for
	 * @return the cache associated with the logical structure type
	 */
	protected LogicalStructureTypeCache getCacheForType(ILogicalStructureType type){
		LogicalStructureTypeCache cache = fCacheForType.get(type);
		if (cache == null){
			cache = new LogicalStructureTypeCache(type);
			fCacheForType.put(type, cache);
		}
		return cache;
	}

	/**
	 * Inner class that caches the known and pending values for a given logical
	 * structure type.
	 */
	class LogicalStructureTypeCache{

		private ILogicalStructureType fType;

		/**
		 * Maps a raw IValue to its calculated logical IValue
		 */
		private Map<IValue, IValue> fKnownValues = new HashMap<>();

		/**
		 * Set of raw IValues that logical values are currently being evaluated for.
		 */
		private Set<IValue> fPendingValues = new HashSet<>();

		public LogicalStructureTypeCache(ILogicalStructureType type){
			fType = type;
		}

		/**
		 * Returns the logical structure value for the given raw value.  If the value has been evaluated
		 * the cached value is returned, otherwise the thread waits until the value is evaluated.
		 *
		 * @param value the raw value
		 * @return the logical value
		 * @exception CoreException if an error occurs computing the value
		 */
		public IValue getLogicalStructure(IValue value) throws CoreException {
			// Check if the value has already been evaluated
			synchronized (fKnownValues) {
				IValue logical = fKnownValues.get(value);
				if (logical != null){
					return logical;
				}
			}
			// Check if the logical structure is currently being evaluated
			synchronized (fPendingValues) {
				if (fPendingValues.contains(value)){
					try {
						fPendingValues.wait();
						return getLogicalStructure(value);
					} catch (InterruptedException e) {
						throw new CoreException(new Status(IStatus.CANCEL, DebugUIPlugin.getUniqueIdentifier(),
								VariablesViewMessages.LogicalStructureCache_0, e));
					}
				} else {
					fPendingValues.add(value);
				}
			}
			// Start the evaluation to get the logical structure
			try {
				IValue result = fType.getLogicalStructure(value);
				synchronized (fKnownValues) {
					fKnownValues.put(value, result);
				}
				return result;
			} finally {
				synchronized (fPendingValues) {
					fPendingValues.remove(value);
					fPendingValues.notifyAll();
				}
			}
		}

		public void dispose() {
			if (!(fType instanceof ILogicalStructureTypeDelegate3)) {
				return;
			}
			ILogicalStructureTypeDelegate3 typeDelegate = (ILogicalStructureTypeDelegate3) fType;
			synchronized (fKnownValues) {
				fKnownValues.values().forEach(typeDelegate::releaseValue);
			}
		}

	}
}
