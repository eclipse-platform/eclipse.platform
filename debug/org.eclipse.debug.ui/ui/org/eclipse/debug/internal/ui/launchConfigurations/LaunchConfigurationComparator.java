/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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

	/**
	 * @see org.eclipse.jface.viewers.ViewerComparator#category(java.lang.Object)
	 */
	@Override
	public int category(Object element) {
		Map<ILaunchConfigurationType, Integer> map = getCategories();
		if (element instanceof ILaunchConfiguration) {
			ILaunchConfiguration configuration = (ILaunchConfiguration) element;
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
