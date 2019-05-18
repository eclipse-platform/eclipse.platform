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
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.team.core.Team;


public class UserStringMappings implements Preferences.IPropertyChangeListener {

	public static final Integer BINARY= Integer.valueOf(Team.BINARY);
	public  static final Integer TEXT= Integer.valueOf(Team.TEXT);
	public static final Integer UNKNOWN= Integer.valueOf(Team.UNKNOWN);


	private static final String PREF_TEAM_SEPARATOR = "\n"; //$NON-NLS-1$

	private final Preferences fPreferences;
	private final String fKey;

	private Map<String, Integer> fMap;

	public UserStringMappings(String key) {
		fKey= key;
		fPreferences= TeamPlugin.getPlugin().getPluginPreferences();
		fPreferences.addPropertyChangeListener(this);
	}

	public Map<String, Integer> referenceMap() {
		if (fMap == null) {
			fMap= loadMappingsFromPreferences();
		}
		return fMap;
	}

	public void addStringMappings(String[] names, int[] types) {
		Assert.isTrue(names.length == types.length);
		final Map<String, Integer> map= referenceMap();

		for (int i = 0; i < names.length; i++) {
			switch (types[i]) {
			case Team.BINARY:    map.put(names[i], BINARY);  break;
			case Team.TEXT:       map.put(names[i], TEXT); break;
			case Team.UNKNOWN:  map.put(names[i], UNKNOWN); break;
			}
		}
		save();
	}

	public void setStringMappings(String [] names, int [] types) {
		Assert.isTrue(names.length == types.length);
		referenceMap().clear();
		addStringMappings(names, types);
	}

	public int getType(String string) {
		if (string == null)
			return Team.UNKNOWN;
		final Integer type= referenceMap().get(string);
		return type != null ? type.intValue() : Team.UNKNOWN;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if(event.getProperty().equals(fKey))
			fMap= null;
	}

	public void save() {
		// Now set into preferences
		final StringBuilder buffer = new StringBuilder();
		final Iterator e = fMap.keySet().iterator();

		while (e.hasNext()) {
			final String filename = (String)e.next();
			buffer.append(filename);
			buffer.append(PREF_TEAM_SEPARATOR);
			final Integer type = fMap.get(filename);
			buffer.append(type);
			buffer.append(PREF_TEAM_SEPARATOR);
		}
		TeamPlugin.getPlugin().getPluginPreferences().setValue(fKey, buffer.toString());
	}

	protected Map<String, Integer> loadMappingsFromPreferences() {
		final Map<String, Integer> result= new HashMap<>();

		if (!fPreferences.contains(fKey))
			return result;

		final String prefTypes = fPreferences.getString(fKey);
		final StringTokenizer tok = new StringTokenizer(prefTypes, PREF_TEAM_SEPARATOR);
		try {
			while (tok.hasMoreElements()) {
				final String name = tok.nextToken();
				final String mode= tok.nextToken();
				result.put(name, Integer.valueOf(mode));
			}
		} catch (NoSuchElementException e) {
		}
		return result;
	}
}
