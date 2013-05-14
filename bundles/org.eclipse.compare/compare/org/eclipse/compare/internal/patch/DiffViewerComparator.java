/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import java.util.Comparator;
import java.util.regex.Pattern;

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.ViewerSorter;

public class DiffViewerComparator extends ViewerSorter {

	public boolean isSorterProperty(Object element, Object property) {
		return false;
	}

	public int category(Object node) {
		if (node instanceof DiffNode) {
			Object o= ((DiffNode) node).getId();
			if (o instanceof DocumentRangeNode)
				return ((DocumentRangeNode) o).getTypeCode();
		}
		return 0;
	}

	protected Comparator getComparator() {
		return new Comparator() {
			public int compare(Object arg0, Object arg1) {
				String label0 = arg0 == null ? "" : arg0.toString(); //$NON-NLS-1$
				String label1 = arg1 == null ? "" : arg1.toString(); //$NON-NLS-1$

				// see org.eclipse.compare.internal.patch.Hunk.getDescription()
				String pattern = "\\d+,\\d+ -> \\d+,\\d+.*"; //$NON-NLS-1$

				if (Pattern.matches(pattern, label0)
						&& Pattern.matches(pattern, label1)) {
					int oldStart0 = Integer.parseInt(label0.split(",")[0]); //$NON-NLS-1$
					int oldStart1 = Integer.parseInt(label1.split(",")[0]); //$NON-NLS-1$

					return oldStart0 - oldStart1;
				}
				return Policy.getComparator().compare(arg0, arg1);
			}
		};
	}
}