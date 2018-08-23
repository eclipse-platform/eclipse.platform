/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.mappings;

import java.util.Comparator;

public class ChangeSetComparator implements Comparator {
	private ChangeSetSorter fSorter= new ChangeSetSorter();

	public int compare(Object o1, Object o2) {
		return fSorter.compare(null, o1, o2);
	}
}
