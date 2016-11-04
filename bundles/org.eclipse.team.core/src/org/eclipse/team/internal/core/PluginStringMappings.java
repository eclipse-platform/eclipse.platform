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

import org.eclipse.core.runtime.*;
import org.eclipse.team.core.Team;

/**
 *
 */
public class PluginStringMappings {

    private final String fExtensionID;
    private final String fAttributeName;

    private SortedMap fMappings;

    public PluginStringMappings(String extensionID, String stringAttributeName) {
        fExtensionID= extensionID;
        fAttributeName= stringAttributeName;
    }

    /**
     * Load all the extension patterns contributed by plugins.
     * @return a map with the patterns
     */
    private SortedMap loadPluginPatterns() {

        final SortedMap result= new TreeMap();

        final TeamPlugin plugin = TeamPlugin.getPlugin();
        if (plugin == null)
            return result;

        final IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(TeamPlugin.ID, fExtensionID);//TeamPlugin.FILE_TYPES_EXTENSION);
        if (extension == null)
            return result;

        final IExtension[] extensions =  extension.getExtensions();

        for (int i = 0; i < extensions.length; i++) {
            IConfigurationElement[] configElements = extensions[i].getConfigurationElements();

            for (int j = 0; j < configElements.length; j++) {

                final String ext = configElements[j].getAttribute(fAttributeName);//"extension");
                final String type = configElements[j].getAttribute("type"); //$NON-NLS-1$
                if (ext == null || type == null)
                    continue;

                if (type.equals("text")) { //$NON-NLS-1$
                    result.put(ext, new Integer(Team.TEXT));
                } else if (type.equals("binary")) { //$NON-NLS-1$
                    result.put(ext, new Integer(Team.BINARY));
                }
            }
        }
        return result;
    }

    public Map referenceMap() {
        if (fMappings == null) {
            fMappings= loadPluginPatterns();
        }
        return fMappings;
    }

    public int getType(String filename) {
        final Map mappings= referenceMap();
        return mappings.containsKey(filename) ? ((Integer)mappings.get(filename)).intValue() : Team.UNKNOWN;
    }
}
