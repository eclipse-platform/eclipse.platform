/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.resources;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.junit.Assert;

/**
 * This class works by recording the current state of given markers,
 * then verifying that the marker deltas accurately reflect the old
 * marker state.
 */
public class MarkerAttributeChangeListener extends Assert implements IResourceChangeListener {
	//Map of (Long(id) -> Map of (String(attribute key) -> Object(attribute value)))
	private Map<Long, Map<String, Object>> attributeMap = new HashMap<>();

	//cache the exception because it can't be thrown from a listener
	private AssertionError error;

	public void expectChanges(IMarker marker) throws CoreException {
		expectChanges(new IMarker[] {marker});
	}

	public void expectChanges(IMarker[] markers) throws CoreException {
		error = null;
		attributeMap.clear();

		for (IMarker marker : markers) {
			attributeMap.put(Long.valueOf(marker.getId()), marker.getAttributes());
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IMarkerDelta[] deltas = event.findMarkerDeltas(null, true);
		try {
			checkDelta(deltas);
		} catch (AssertionError e) {
			error = e;
		} catch (Throwable e) {
			error = new AssertionError(e);
		}
	}

	private void checkDelta(IMarkerDelta[] deltas) {
		int expectedCount = attributeMap.size();
		int actualCount = deltas.length;
		assertEquals("wrong number of changes", expectedCount, actualCount);
		for (IMarkerDelta delta : deltas) {
			Map<String, Object> expected = attributeMap.get(Long.valueOf(delta.getId()));
			Map<String, Object> actual = delta.getAttributes();
			assertEquals("Changes different from expecations", expected, actual);
		}
	}

	/**
	 * Verifies that the delta was as expected.  Throws an appropriate
	 * assertion failure if the delta did not meet expectations.
	 */
	public void verifyChanges() {
		if (error != null) {
			throw error;
		}
	}
}
