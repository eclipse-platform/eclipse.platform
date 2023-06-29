/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

import java.util.Comparator;

import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.ui.IWorkingSet;

/**
 * Comparator for refresh scope launch configuration attribute
 * <code>ATTR_REFRESH_SCOPE</code>.
 */
public class WorkingSetComparator implements Comparator<String> {

	@Override
	public int compare(String o1, String o2) {
		String one= o1;
		String two= o2;
		if (one == null || two == null) {
			if (one == two) {
				return 0;
			}
			return -1;
		}
		if (one.startsWith("${working_set:") && two.startsWith("${working_set:")) {		  //$NON-NLS-1$//$NON-NLS-2$
			IWorkingSet workingSet1 = RefreshTab.getWorkingSet(one);
			IWorkingSet workingSet2 = RefreshTab.getWorkingSet(two);
			if (workingSet1 == null || workingSet2 == null) {
				if (workingSet1 == workingSet2) {
					return 0;
				}
				return -1;
			}
			if (workingSet1.equals(workingSet2)) {
				return 0;
			}
			return -1;
		}
		return one.compareTo(two);
	}
}
