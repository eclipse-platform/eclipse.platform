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
import org.eclipse.core.runtime.Path;

/**
 * Testing interface containing various paths.
 */
interface IPathConstants {

	/**
	 * The following paths are used in the tree created by
	 * ElementTreeTestUtilities.createTestElementTree()
	 */
	IPath root = Path.ROOT;
	IPath solution = root.append("solution");
	IPath project1 = solution.append("project1");
	IPath project2 = solution.append("project2");
	IPath file1 = project2.append("file1");
	IPath folder1 = project2.append("folder1");
	IPath folder2 = project2.append("folder2");

	IPath file2 = folder1.append("file2");
	IPath folder3 = folder1.append("folder3");
	IPath folder4 = folder1.append("folder4");

	IPath file3 = folder3.append("file3");
}
