/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Plugins with prebuilt search indexes.
 *
 */
public class PrebuiltIndexes {
	private SearchIndex targetIndex;

	/**
	 * Set of PluginIndex
	 */
	private Set<PluginIndex> set = new HashSet<>();

	PrebuiltIndexes(SearchIndex targetIndex) {
		super();
		this.targetIndex = targetIndex;
	}

	void add(String plugin, String path) {
		set.add(new PluginIndex(plugin, path, targetIndex));
	}

	/**
	 * Removes Plugin indexes with no index
	 */
	private void trim() {
		List<PluginIndex> indexes = new ArrayList<>(set);
		for (int i = 0; i < indexes.size();) {
			PluginIndex index = indexes.get(i);
			if (index.getPaths().isEmpty()) {
				set.remove(index);
			}
			i++;
		}
	}

	public PluginIndex[] getIndexes() {
		trim();
		return set.toArray(new PluginIndex[set.size()]);
	}
}
