/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.watson;

import org.eclipse.core.runtime.IPath;

/**
 * Testing interface containing various paths.
 */
interface IPathConstants {

	/**
	 * The following paths are used in the tree created by
	 * ElementTreeTestUtilities.createTestElementTree()
	 */
	final IPath root = IPath.ROOT;
	final IPath solution = root.append("solution");
	final IPath project1 = solution.append("project1");
	final IPath project2 = solution.append("project2");
	final IPath file1 = project2.append("file1");
	final IPath folder1 = project2.append("folder1");
	final IPath folder2 = project2.append("folder2");

	final IPath file2 = folder1.append("file2");
	final IPath folder3 = folder1.append("folder3");
	final IPath folder4 = folder1.append("folder4");

	final IPath file3 = folder3.append("file3");
}
