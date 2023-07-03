/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.help.internal.workingset;

import java.text.Collator;
import java.util.Comparator;

/**
 * Compares two working sets by name.
 */
public class WorkingSetComparator implements Comparator<WorkingSet> {
	private Collator fCollator = Collator.getInstance();

	@Override
	public int compare(WorkingSet o1,WorkingSet o2) {
		String name1 = null;
		String name2 = null;

		if (o1 != null)
			name1 = o1.getName();

		if (o2 != null)
			name2 = o2.getName();

		if (name1 == null || name2 == null)
			return -1;

		return fCollator.compare(name1, name2);
	}
}
