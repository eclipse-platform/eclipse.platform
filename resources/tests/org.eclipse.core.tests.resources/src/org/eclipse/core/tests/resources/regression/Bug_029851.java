/*******************************************************************************
 *  Copyright (c) 2004, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests regression of bug 25457.  In this case, attempting to move a project
 * that is only a case change, where the move fails due to another handle being
 * open on a file in the hierarchy, would cause deletion of the source.
 */
public class Bug_029851 extends ResourceTest {

	private Collection<String> createChildren(int breadth, int depth, IPath prefix) {
		ArrayList<String> result = new ArrayList<>();
		for (int i = 0; i < breadth; i++) {
			IPath child = prefix.append(Integer.toString(i)).addTrailingSeparator();
			result.add(child.toString());
			if (depth > 0) {
				result.addAll(createChildren(breadth, depth - 1, child));
			}
		}
		return result;
	}

	@Override
	public String[] defineHierarchy() {
		int depth = 3;
		int breadth = 3;
		IPath prefix = IPath.fromOSString("/a/");
		Collection<String> result = createChildren(breadth, depth, prefix);
		result.add(prefix.toString());
		return result.toArray(new String[0]);
	}

	public void test() {
		createHierarchy();
		final QualifiedName key = new QualifiedName("local", getUniqueString());
		final String value = getUniqueString();
		IResourceVisitor visitor = resource -> {
			resource.setPersistentProperty(key, value);
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("1.0", e);
		}
	}
}
