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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests regression of bug 97608 - error restoring workspace
 * after writing marker with value that is too long.
 */
public class Bug_097608 {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	/**
	 * Tests that creating a marker with very long value causes failure.
	 */
	@Test
	public void testBug() throws CoreException {
		char[] chars = new char[40000];
		Arrays.fill(chars, 'a');
		String originalValue = new String(chars);
		IMarker marker = ResourcesPlugin.getWorkspace().getRoot().createMarker(IMarker.PROBLEM);
		// first try a long value under the limit
		marker.setAttribute(IMarker.MESSAGE, originalValue);
		//now create a marker with illegal length attribute
		String firstChangedValue = originalValue + originalValue;
		assertThrows(RuntimeException.class, () -> marker.setAttribute(IMarker.MESSAGE, firstChangedValue));
		//try a string with less than 65536 characters whose UTF encoding exceeds the limit
		Arrays.fill(chars, (char) 0x0800);
		String secondChangedValue = new String(chars);
		assertThrows(RuntimeException.class, () -> marker.setAttribute(IMarker.MESSAGE, secondChangedValue));
	}

	/**
	 * Tests that creating a marker with very long value causes failure.
	 */
	@Test
	public void testBug2() throws CoreException {
		char[] chars = new char[40000];
		Arrays.fill(chars, 'a');
		String value = new String(chars);

		Map<String, String> markerAttributes = new HashMap<>();
		markerAttributes.put(IMarker.MESSAGE, value);

		IMarker marker = ResourcesPlugin.getWorkspace().getRoot().createMarker(IMarker.PROBLEM);
		// first try a long value under the limit
		marker.setAttributes(markerAttributes);
		//now create a marker with illegal length attribute
		value = value + value;
		markerAttributes.put(IMarker.MESSAGE, value);
		assertThrows(RuntimeException.class, () -> marker.setAttributes(markerAttributes));
		//try a string with less than 65536 characters whose UTF encoding exceeds the limit
		Arrays.fill(chars, (char) 0x0800);
		value = new String(chars);
		markerAttributes.put(IMarker.MESSAGE, value);
		assertThrows(RuntimeException.class, () -> marker.setAttributes(markerAttributes));
	}

}
