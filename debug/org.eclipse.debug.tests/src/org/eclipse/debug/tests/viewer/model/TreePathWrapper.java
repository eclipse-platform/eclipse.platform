/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
package org.eclipse.debug.tests.viewer.model;


import org.eclipse.jface.viewers.TreePath;
import org.junit.Assert;

/**
 * Utility for comparing TreePath objects in unit tests.  This wrapper prints the tree
 * paths in exception showing contexts of the paths.
 *
 * @since 3.7
 */
public class TreePathWrapper {
	private final TreePath fPath;

	public TreePathWrapper(TreePath path) {
		fPath = path;
	}

	@Override
	public int hashCode() {
		return fPath.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof TreePathWrapper &&
			   fPath.equals( ((TreePathWrapper)obj).fPath );
	}

	@Override
	public String toString() {
		if (fPath.getSegmentCount() == 0) {
			return "TreePath:EMPTY"; //$NON-NLS-1$
		}

		StringBuilder buf = new StringBuilder("TreePath:["); //$NON-NLS-1$

		for (int i = 0; i < fPath.getSegmentCount(); i++) {
			if (i != 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			buf.append(fPath.getSegment(i));
		}
		buf.append(']');
		return buf.toString();
	}

	/**
	 * Asserts that the two given tree paths are the same.  In case of failure, the
	 * generated exception will contain a printout of the tree paths' contents.
	 */
	public static void assertEqual(TreePath expected, TreePath actual) {
		Assert.assertEquals(
			expected != null ? new TreePathWrapper(expected) : null,
			actual != null ? new TreePathWrapper(actual) : null);
	}
}