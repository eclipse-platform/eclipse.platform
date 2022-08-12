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
package org.eclipse.core.tests.internal.resources;

import java.io.*;
import java.util.Map;
import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.ResourceTest;

public class ResourceInfoTest extends ResourceTest {

	static public void assertEquals(String message, byte[] expected, byte[] actual) {
		if (expected == null && actual == null) {
			return;
		}
		if (expected == null || actual == null) {
			assertTrue(message, false);
		}
		assertEquals(message, expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(message, expected[i], actual[i]);
		}
	}

	static public void assertEquals(String message, Map<?, ?> expected, Map<?, ?> actual) {
		if (expected == null && actual == null) {
			return;
		}
		if (expected == null || actual == null) {
			assertTrue(message, false);
		}
		assertEquals(message, expected.size(), actual.size());
		for (Map.Entry<?, ?> entry : expected.entrySet()) {
			Object key = entry.getKey();
			assertTrue(message, actual.containsKey(key));
			Object expectedValue = entry.getValue();
			Object actualValue = actual.get(key);
			if (expectedValue instanceof byte[] && actualValue instanceof byte[]) {
				assertEquals(message, (byte[]) expectedValue, (byte[]) actualValue);
			} else {
				assertEquals(message, expectedValue, actualValue);
			}
		}
	}

	static public void assertEquals(String message, ResourceInfo expected, ResourceInfo actual) {
		if (expected == null && actual == null) {
			return;
		}
		if (expected == null || actual == null) {
			assertTrue(message, false);
		}
		boolean different = false;
		different &= expected.getFlags() == actual.getFlags();
		different &= expected.getContentId() == actual.getContentId();
		different &= expected.getModificationStamp() == actual.getModificationStamp();
		different &= expected.getNodeId() == actual.getNodeId();
		different &= expected.getLocalSyncInfo() == actual.getLocalSyncInfo();
		// TODO sync info isn't serialized by this class so don't expect it to be loaded
		//	assertEquals(message, expected.getSyncInfo(false), actual.getSyncInfo(false));
		different &= expected.getMarkerGenerationCount() == actual.getMarkerGenerationCount();
		if (different) {
			assertTrue(message, false);
		}
	}

	public void testSerialization() {
		ByteArrayInputStream input = null;
		ByteArrayOutputStream output = null;
		ResourceInfo info = new ResourceInfo();
		ResourceInfo newInfo = new ResourceInfo();

		// write out an empty info
		try {
			output = new ByteArrayOutputStream();
			info.writeTo(new DataOutputStream(output));
		} catch (IOException e) {
			fail("1.0", e);
		}
		try {
			input = new ByteArrayInputStream(output.toByteArray());
			newInfo.readFrom(0, new DataInputStream(input));
		} catch (IOException e) {
			fail("1.1", e);
		}
		assertEquals("1.2", info, newInfo);

		// write and info with syncinfo set
		info = new ResourceInfo();
		// set no bytes
		QualifiedName qname = new QualifiedName("org.eclipse.core.tests", "myTest1");
		byte[] bytes = new byte[0];
		info.setSyncInfo(qname, bytes);
		// set some real bytes
		qname = new QualifiedName("org.eclipse.core.tests", "myTest2");
		bytes = new byte[] {0, 1, 2, 3, 4, 5};
		info.setSyncInfo(qname, bytes);
		try {
			output = new ByteArrayOutputStream();
			info.writeTo(new DataOutputStream(output));
		} catch (IOException e) {
			fail("2.0", e);
		}
		try {
			newInfo = new ResourceInfo();
			input = new ByteArrayInputStream(output.toByteArray());
			newInfo.readFrom(0, new DataInputStream(input));
		} catch (IOException e) {
			fail("2.1", e);
		}
		assertEquals("2.2", info, newInfo);
	}
}
