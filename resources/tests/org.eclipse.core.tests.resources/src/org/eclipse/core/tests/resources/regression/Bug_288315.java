/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests regression of bug 288315 - MarkerInfo.setAttributes(Map map) calls
 * checkValidAttribute which throws IllegalArgumentException.
 */
public class Bug_288315 extends ResourceTest {

	/**
	 * Tests that creating a marker with very long value causes failure.
	 */
	public void testBug() throws CoreException {
		char[] chars = new char[65537];
		Arrays.fill(chars, 'a');
		String value1 = new String(chars);
		Float value2 = Float.valueOf(1.1f);

		IMarker nonPersistentMarker = ResourcesPlugin.getWorkspace().getRoot().createMarker(IMarker.MARKER);
		nonPersistentMarker.setAttribute(IMarker.MESSAGE, value1);
		nonPersistentMarker.setAttribute(IMarker.MESSAGE, value2);
		nonPersistentMarker.delete();

		IMarker persistentMarker = ResourcesPlugin.getWorkspace().getRoot().createMarker(IMarker.PROBLEM);
		assertThrows(RuntimeException.class, () -> persistentMarker.setAttribute(IMarker.MESSAGE, value1));
		assertThrows(RuntimeException.class, () -> persistentMarker.setAttribute(IMarker.MESSAGE, value2));
		persistentMarker.delete();
	}

}