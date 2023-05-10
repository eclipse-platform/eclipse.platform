/*******************************************************************************
 * Copyright (c) 2012, 2023 IBM Corporation and others.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Listeners are held wrapped in weak references and are removed if no other [strong] reference
 * exists.
 */
public class WeakGroupedListenerList {

	private final Map<String, Set<Computation>> listeners = new HashMap<>();

	public synchronized void add(String groupName, Computation computation) {
		Objects.requireNonNull(computation);
		listeners.computeIfAbsent(groupName,
						k -> Collections.newSetFromMap(new WeakHashMap<>()))
				.add(computation);
	}

	public synchronized void remove(Computation computation) {
		Collection<Set<Computation>> allListeners = listeners.values();
		for (Set<Computation> group : allListeners) {
			group.remove(computation);
		}
	}

	public synchronized Set<String> getNames() {
		Set<String> groupNames = listeners.keySet(); // clone internal name list
		Set<String> usedNames = new HashSet<>(groupNames.size());
		usedNames.addAll(groupNames);
		return usedNames;
	}

	public synchronized void clear() {
		listeners.clear();
	}

	public synchronized Set<Computation> getListeners() {
		Collection<Set<Computation>> groups = listeners.values();
		Set<Computation> result = new HashSet<>();
		for (Set<Computation> computations : groups) {
			for (Computation computation : computations) {
				if (computation.isValid()) {
					result.add(computation);
				}
			}
		}
		return result;
	}

	public synchronized Set<Computation> getListeners(String groupName) {
		Set<Computation> computations = listeners.get(groupName);
		if (computations == null)
			return null;
		Set<Computation> result = new HashSet<>(computations.size());
		for (Computation computation : computations) {
			if (computation.isValid()) {
				result.add(computation);
			}
		}
		return result;
	}

	public synchronized void cleanup() {
		Set<Entry<String, Set<Computation>>> entries = listeners.entrySet();
		entries.removeIf(entry -> {
			Set<Computation> computations = entry.getValue();
			computations.removeIf(computation -> !computation.isValid());
			return computations.isEmpty();
		});
	}

}
