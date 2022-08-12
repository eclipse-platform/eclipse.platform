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

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.team.core.Team;

/**
 *
 */
public class PluginStringMappings {

	private final String fExtensionID;
	private final String fAttributeName;

	private SortedMap<String, Integer> fMappings;

	public PluginStringMappings(String extensionID, String stringAttributeName) {
		fExtensionID= extensionID;
		fAttributeName= stringAttributeName;
	}

	/**
	 * Load all the extension patterns contributed by plugins.
	 * @return a map with the patterns
	 */
	private SortedMap<String, Integer> loadPluginPatterns() {

		final SortedMap<String, Integer> result= new TreeMap<>();

		final TeamPlugin plugin = TeamPlugin.getPlugin();
		if (plugin == null)
			return result;

		final IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(TeamPlugin.ID, fExtensionID);//TeamPlugin.FILE_TYPES_EXTENSION);
		if (extension == null)
			return result;

		final IExtension[] extensions =  extension.getExtensions();

		for (IExtension e : extensions) {
			IConfigurationElement[] configElements = e.getConfigurationElements();
			for (IConfigurationElement configElement : configElements) {
				final String ext = configElement.getAttribute(fAttributeName); //"extension");
				final String type = configElement.getAttribute("type"); //$NON-NLS-1$
				if (ext == null || type == null)
					continue;
				if (type.equals("text")) { //$NON-NLS-1$
					result.put(ext, Integer.valueOf(Team.TEXT));
				} else if (type.equals("binary")) { //$NON-NLS-1$
					result.put(ext, Integer.valueOf(Team.BINARY));
				}
			}
		}
		return result;
	}

	public Map<String, Integer> referenceMap() {
		if (fMappings == null) {
			fMappings= loadPluginPatterns();
		}
		return fMappings;
	}

	public int getType(String filename) {
		final Map<String, Integer> mappings= referenceMap();
		return mappings.containsKey(filename) ? mappings.get(filename).intValue() : Team.UNKNOWN;
	}
}
