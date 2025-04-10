/*******************************************************************************
 * Copyright (c) 2020 Torbjörn Svensson and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Torbjörn Svensson - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Filters out the instance of ILaunchCOnfiguration where the, possibly nested,
 * launch configuration files are stored. If the launch configuration is local,
 * it's simply returned.
 */
public class UniqueLaunchConfigurationFileFilter extends ViewerFilter {
	@Override
	public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
		int size = elements.length;
		if (size == 0) {
			return elements;
		}

		List<Object> filteredElements = new ArrayList<>(size);

		Map<String, List<ILaunchConfiguration>> configPathMap = new HashMap<>();
		for (Object element : elements) {
			if (element instanceof ILaunchConfiguration) {
				ILaunchConfiguration config = (ILaunchConfiguration) element;

				String path = toLaunchFileLocation(config);
				if (!configPathMap.containsKey(path)) {
					configPathMap.put(path, new ArrayList<>());
				}
				configPathMap.get(path).add(config);
			} else {
				filteredElements.add(element);
			}
		}

		for (Entry<String, List<ILaunchConfiguration>> entry : configPathMap.entrySet()) {
			List<ILaunchConfiguration> configsWithSamePath = entry.getValue();
			if (entry.getKey() == null) {
				// Local configurations ends up here, add them without further filter
				filteredElements.addAll(configsWithSamePath);
			} else if (configsWithSamePath.size() == 1) {
				// Only one configuration, add it
				filteredElements.add(configsWithSamePath.get(0));
			} else if (configsWithSamePath.size() > 1) {
				// More than one config with same path.
				// Order them with the shortest project relative path first
				configsWithSamePath.sort((o1, o2) -> {
					IPath path1 = o1.getFile().getProjectRelativePath();
					IPath path2 = o2.getFile().getProjectRelativePath();
					return Integer.compare(path1.segmentCount(), path2.segmentCount());
				});
				// Use the configuration with the shortest path.
				// The other configurations in the list are "identical" but with
				// the container set to one of the parent project(s) in the
				// workspace hierarchy.
				filteredElements.add(configsWithSamePath.get(0));
			}
		}

		return filteredElements.toArray();
	}

	private String toLaunchFileLocation(ILaunchConfiguration config) {
		if (!config.isLocal()) {
			IFile file = config.getFile();
			if (file != null) {
				IPath path = file.getLocation();
				if (path != null) {
					return path.toOSString();
				}
			}
		}
		return null;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		return true;
	}
}
