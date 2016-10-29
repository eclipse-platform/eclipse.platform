/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.internal.core;

import java.util.*;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.team.core.Team;


public class UserStringMappings implements Preferences.IPropertyChangeListener {

    public static final Integer BINARY= new Integer(Team.BINARY);
    public  static final Integer TEXT= new Integer(Team.TEXT);
    public static final Integer UNKNOWN= new Integer(Team.UNKNOWN);


    private static final String PREF_TEAM_SEPARATOR = "\n"; //$NON-NLS-1$

    private final Preferences fPreferences;
    private final String fKey;

    private Map fMap;

    public UserStringMappings(String key) {
        fKey= key;
        fPreferences= TeamPlugin.getPlugin().getPluginPreferences();
        fPreferences.addPropertyChangeListener(this);
    }

    public Map referenceMap() {
        if (fMap == null) {
            fMap= loadMappingsFromPreferences();
        }
        return fMap;
    }

    public void addStringMappings(String[] names, int[] types) {
        Assert.isTrue(names.length == types.length);
        final Map map= referenceMap();

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
        final Integer type= (Integer)referenceMap().get(string);
        return type != null ? type.intValue() : Team.UNKNOWN;
    }

    public void propertyChange(PropertyChangeEvent event) {
        if(event.getProperty().equals(fKey))
            fMap= null;
    }

    public void save() {
        // Now set into preferences
        final StringBuffer buffer = new StringBuffer();
        final Iterator e = fMap.keySet().iterator();

        while (e.hasNext()) {
            final String filename = (String)e.next();
            buffer.append(filename);
            buffer.append(PREF_TEAM_SEPARATOR);
            final Integer type = (Integer)fMap.get(filename);
            buffer.append(type);
            buffer.append(PREF_TEAM_SEPARATOR);
        }
        TeamPlugin.getPlugin().getPluginPreferences().setValue(fKey, buffer.toString());
    }

    protected Map loadMappingsFromPreferences() {
        final Map result= new HashMap();

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
