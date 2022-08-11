/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.resources;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.PreferenceModifyListener;

public class ProjectPreferenceModifyListener extends PreferenceModifyListener {
	/*
	 * Return the segment from the given path or null.
	 * "segment" parameter is 0-based.
	 */
	private static String getSegment(String path, int segment) {
		int start = path.indexOf(IPath.SEPARATOR) == 0 ? 1 : 0;
		int end = path.indexOf(IPath.SEPARATOR, start);
		if (end == path.length() - 1) {
			end = -1;
		}
		for (int i = 0; i < segment; i++) {
			if (end == -1) {
				return null;
			}
			start = end + 1;
			end = path.indexOf(IPath.SEPARATOR, start);
		}
		if (end == -1) {
			end = path.length();
		}
		return path.substring(start, end);
	}

	private static boolean equalsQualifier(IEclipsePreferences node, String qualifier) {
		return qualifier.equals(getSegment(node.absolutePath(), 2));
	}

	@Override
	public IEclipsePreferences preApply(IEclipsePreferences node) {
		if (equalsQualifier(node, "test.load.is.import")) {
			return testLoadIsImport(node);
		}
		return super.preApply(node);
	}

	private IEclipsePreferences testLoadIsImport(IEclipsePreferences node) {
		node.put("key", "new value");
		return node;
	}

}
