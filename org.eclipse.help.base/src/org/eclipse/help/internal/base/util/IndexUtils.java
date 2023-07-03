/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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

package org.eclipse.help.internal.base.util;

import org.eclipse.help.IIndex;
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IIndexSee;
import org.eclipse.help.IIndexSubpath;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.UAElement;

public class IndexUtils {

	/**
	 * Return a path representing all of the elements in the path of the see target
	 * For example if the see targets Eclipse / PDE the result will be an array
	 * of length 2 containing the entries for Eclipse and for PDE in that order
	 * @param index The index containing this see element or null to discover the index
	 * in this routine.
	 */
	public static IIndexEntry[] findSeeTargets(IUAElement index, IIndexSee see, int depth) {
		if (index == null && see instanceof UAElement) {
			UAElement ancestor = ((UAElement)see).getParentElement();
			while (!(ancestor instanceof IIndex)) {
				if (ancestor == null) {
					return new IIndexEntry[0];
				}
				ancestor = ancestor.getParentElement();
			}
			index = ancestor;
		}
		String[] path = getPath(see);
		IUAElement[] children = index.getChildren();
		for (IUAElement element : children) {
			if (element instanceof IIndexEntry) {
				IIndexEntry indexEntry = (IIndexEntry) element;
				String entryKeyword = indexEntry.getKeyword();
				if (path[depth].equals(entryKeyword)) {
					if (path.length == depth + 1) {
						return new IIndexEntry[] { indexEntry };
					}
					IIndexEntry[] targets = findSeeTargets(indexEntry, see, depth + 1);
					IIndexEntry[] result = new IIndexEntry[targets.length + 1];
					result[0] = indexEntry;
					System.arraycopy(targets, 0, result, 1, targets.length);
					return result;
				}
			}
		}
		return new IIndexEntry[0];
	}

	public static String[] getPath(IIndexSee see) {
		IIndexSubpath[] subpaths = see.getSubpathElements();
		String[] result = new String[1 + subpaths.length];
		result[0] = see.getKeyword();
		for (int i = 0; i < subpaths.length; i++) {
			result[i + 1] = subpaths[i].getKeyword();
		}
		return result;
	}

}
