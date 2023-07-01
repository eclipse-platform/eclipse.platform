/*******************************************************************************
 * Copyright (c) 2000, 2017, 2023 IBM Corporation and others.
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
 *     Nikifor Fedorov (ArSysOp) - Use equinox preferences APIs in UserStringMappings #497
 *******************************************************************************/

package org.eclipse.team.internal.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.team.core.Team;
import org.osgi.service.prefs.BackingStoreException;

public class UserStringMappings {

	public static final Integer BINARY = Integer.valueOf(Team.BINARY);
	public static final Integer TEXT = Integer.valueOf(Team.TEXT);
	public static final Integer UNKNOWN = Integer.valueOf(Team.UNKNOWN);

	private static final String PREF_TEAM_SEPARATOR = "\n"; //$NON-NLS-1$
	private static final String EMPTY_PREF = ""; //$NON-NLS-1$

	private final String fKey;

	private Map<String, Integer> fMap;

	public UserStringMappings(String key) {
		fKey = key;
		InstanceScope.INSTANCE.getNode(TeamPlugin.ID).addPreferenceChangeListener(this::preferenceChanged);
	}

	public Map<String, Integer> referenceMap() {
		if (fMap == null) {
			fMap = loadMappingsFromPreferences();
		}
		return fMap;
	}

	public void addStringMappings(String[] names, int[] types) {
		Assert.isTrue(names.length == types.length);
		final Map<String, Integer> map = referenceMap();

		for (int i = 0; i < names.length; i++) {
			switch (types[i]) {
			case Team.BINARY:
				map.put(names[i], BINARY);
				break;
			case Team.TEXT:
				map.put(names[i], TEXT);
				break;
			case Team.UNKNOWN:
				map.put(names[i], UNKNOWN);
				break;
			}
		}
		save();
	}

	public void setStringMappings(String[] names, int[] types) {
		Assert.isTrue(names.length == types.length);
		referenceMap().clear();
		addStringMappings(names, types);
	}

	public int getType(String string) {
		if (string == null)
			return Team.UNKNOWN;
		final Integer type = referenceMap().get(string);
		return type != null ? type.intValue() : Team.UNKNOWN;
	}

	public void save() {
		// Now set into preferences
		final StringBuilder buffer = new StringBuilder();
		final Iterator e = fMap.keySet().iterator();

		while (e.hasNext()) {
			final String filename = (String) e.next();
			buffer.append(filename);
			buffer.append(PREF_TEAM_SEPARATOR);
			final Integer type = fMap.get(filename);
			buffer.append(type);
			buffer.append(PREF_TEAM_SEPARATOR);
		}
		try {
			IEclipsePreferences node = InstanceScope.INSTANCE.getNode(TeamPlugin.ID);
			node.put(fKey, buffer.toString());
			node.flush();
		} catch (BackingStoreException ex) {
			TeamPlugin.log(IStatus.ERROR, ex.getMessage(), ex);
		}
	}

	protected Map<String, Integer> loadMappingsFromPreferences() {
		final Map<String, Integer> result = new HashMap<>();

		if (!nodeAccessibleAndExists(fKey))
			return result;
		final StringTokenizer tok = new StringTokenizer(mappings(), PREF_TEAM_SEPARATOR);
		try {
			while (tok.hasMoreElements()) {
				final String name = tok.nextToken();
				final String mode = tok.nextToken();
				result.put(name, Integer.valueOf(mode));
			}
		} catch (NoSuchElementException e) {
		}
		return result;
	}

	private String mappings() {
		return Optional.ofNullable(InstanceScope.INSTANCE.getNode(TeamPlugin.ID)) //
				.map(node -> node.get(fKey, null)) //
				.orElse(EMPTY_PREF);
	}

	private boolean nodeAccessibleAndExists(String key) {
		try {
			IEclipsePreferences node = InstanceScope.INSTANCE.getNode(TeamPlugin.ID);
			return Stream.of(node.keys()).anyMatch(candidate -> key.equals(candidate));
		} catch (BackingStoreException e) {
			TeamPlugin.log(IStatus.ERROR, e.getMessage(), e);
			return false;
		}
	}

	private void preferenceChanged(PreferenceChangeEvent event) {
		if (fKey.equals(event.getKey()))
			fMap = null;
	}
}
