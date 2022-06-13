/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
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

package org.eclipse.e4.core.internal.contexts;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Listeners are held wrapped in weak references and are removed if no other [strong] reference
 * exists.
 */
public class WeakGroupedListenerList {

	public static class WeakComputationReference extends WeakReference<Computation> {

		final private int hashCode;

		public WeakComputationReference(Computation computation) {
			super(computation);
			hashCode = computation.hashCode();
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (!WeakComputationReference.class.equals(obj.getClass()))
				return super.equals(obj);
			Computation computation = get();
			Computation otherComputation = ((WeakComputationReference) obj).get();
			if (computation == null && otherComputation == null)
				return true;
			if (computation == null || otherComputation == null)
				return false;
			return computation.equals(otherComputation);
		}
	}

	private Map<String, HashSet<WeakComputationReference>> listeners = new HashMap<>(10, 0.8f);

	synchronized public void add(String groupName, Computation computation) {
		HashSet<WeakComputationReference> nameListeners = listeners.get(groupName);
		if (nameListeners == null) {
			nameListeners = new HashSet<>(30, 0.75f);
			nameListeners.add(new WeakComputationReference(computation));
			listeners.put(groupName, nameListeners);
		}
		nameListeners.add(new WeakComputationReference(computation));
	}

	synchronized public void remove(Computation computation) {
		WeakComputationReference ref = new WeakComputationReference(computation);
		Collection<HashSet<WeakComputationReference>> allListeners = listeners.values();
		for (HashSet<WeakComputationReference> group : allListeners) {
			group.remove(ref);
		}
	}

	synchronized public Set<String> getNames() {
		Set<String> tmp = listeners.keySet(); // clone internal name list
		Set<String> usedNames = new HashSet<>(tmp.size());
		usedNames.addAll(tmp);
		return usedNames;
	}

	synchronized public void clear() {
		listeners.clear();
	}

	synchronized public Set<Computation> getListeners() {
		Collection<HashSet<WeakComputationReference>> collection = listeners.values();
		Set<Computation> result = new HashSet<>();
		for (HashSet<WeakComputationReference> set : collection) {
			for (Iterator<WeakComputationReference> i = set.iterator(); i.hasNext();) {
				WeakComputationReference ref = i.next();
				Computation computation = ref.get();
				if (computation == null || !computation.isValid()) {
					i.remove(); // do a clean-up while we are here
				} else
					result.add(computation);
			}
		}
		return result;
	}

	synchronized public Set<Computation> getListeners(String groupName) {
		HashSet<WeakComputationReference> tmp = listeners.get(groupName);
		if (tmp == null)
			return null;
		Set<Computation> result = new HashSet<>(tmp.size());

		for (Iterator<WeakComputationReference> i = tmp.iterator(); i.hasNext();) {
			WeakComputationReference ref = i.next();
			Computation computation = ref.get();
			if (computation == null || !computation.isValid()) {
				i.remove(); // do a clean-up while we are here
			} else
				result.add(computation);
		}
		return result;
	}

	synchronized public void cleanup() {
		boolean cleanGroups = false;
		for (HashSet<WeakComputationReference> set : listeners.values()) {
			for (Iterator<WeakComputationReference> i = set.iterator(); i.hasNext();) {
				WeakComputationReference ref = i.next();
				Computation computation = ref.get();
				if (computation == null || !computation.isValid())
					i.remove();
			}
			if (set.isEmpty())
				cleanGroups = true;
		}
		if (cleanGroups) {
			Set<Entry<String, HashSet<WeakComputationReference>>> entries = listeners.entrySet();
			for (Iterator<Entry<String, HashSet<WeakComputationReference>>> i = entries.iterator(); i.hasNext();) {
				Entry<String, HashSet<WeakComputationReference>> entry = i.next();
				HashSet<WeakComputationReference> value = entry.getValue();
				if (value == null || value.isEmpty())
					i.remove();
			}
		}
	}

}
