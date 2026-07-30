/*******************************************************************************
 * Copyright (c) 2007, 2026 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.launchConfigurations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.model.WorkbenchViewerComparator;

/**
 * Groups configurations by type.
 *
 * @since 3.3
 */
public class LaunchConfigurationComparator extends WorkbenchViewerComparator {

	/**
	 * the map of categories of <code>ILaunchConfigurationType</code>s to <code>Integer</code>s entries
	 */
	private static Map<ILaunchConfigurationType, Integer> fgCategories;
	private final Map<ILaunchConfiguration, Integer> recentLaunches = new HashMap<>();

	public LaunchConfigurationComparator(String groupIdentifier) {
		LaunchHistory history = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchHistory(groupIdentifier);
		if (history != null) {
			ILaunchConfiguration[] launches = history.getCompleteLaunchHistory();
			for (int i = 0; i < launches.length; i++) {
				recentLaunches.put(launches[i], Integer.valueOf(i));
			}
		}
	}

	public LaunchConfigurationComparator() {
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerComparator#category(java.lang.Object)
	 */
	@Override
	public int category(Object element) {
		Map<ILaunchConfigurationType, Integer> map = getCategories();
		if (element instanceof ILaunchConfiguration configuration) {
			try {
				Integer i = map.get(configuration.getType());
				if (i != null) {
					return i.intValue();
				}
			} catch (CoreException e) {
			}
		}
		return map.size();
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof ILaunchConfiguration c1 && e2 instanceof ILaunchConfiguration c2) {
			Integer recent1 = recentLaunches.get(c1);
			Integer recent2 = recentLaunches.get(c2);
			if (recent1 != null || recent2 != null) {
				if (recent1 == null) {
					return 1;
				}
				if (recent2 == null) {
					return -1;
				}
				int result = recent1.compareTo(recent2);
				if (result != 0) {
					return result;
				}
			}
		}
		return super.compare(viewer, e1, e2);
	}

	/**
	 * Returns the map of categories
	 * @return the map of categories
	 */
	private Map<ILaunchConfigurationType, Integer> getCategories() {
		if (fgCategories == null) {
			fgCategories = new HashMap<>();
			List<ILaunchConfigurationType> types = Arrays.asList(DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationTypes());
			Collections.sort(types, (o1, o2) -> {
				ILaunchConfigurationType t1 = o1;
				ILaunchConfigurationType t2 = o2;
				return t1.getName().compareTo(t2.getName());
			});
			Iterator<ILaunchConfigurationType> iterator = types.iterator();
			int i = 0;
			while (iterator.hasNext()) {
				fgCategories.put(iterator.next(), Integer.valueOf(i));
				i++;
			}
		}
		return fgCategories;
	}
}
