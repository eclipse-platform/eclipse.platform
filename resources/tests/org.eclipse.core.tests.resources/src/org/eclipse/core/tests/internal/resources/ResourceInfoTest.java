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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

public class ResourceInfoTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	static public void assertEquals(ResourceInfo expected, ResourceInfo actual) {
		if (expected == null && actual == null) {
			return;
		}
		if (expected == null || actual == null) {
			assertTrue(false);
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
			assertTrue(false);
		}
	}

	@Test
	public void testSerialization() throws IOException {
		ByteArrayInputStream input = null;
		ByteArrayOutputStream output = null;
		ResourceInfo info = new ResourceInfo();
		ResourceInfo newInfo = new ResourceInfo();

		// write out an empty info
		output = new ByteArrayOutputStream();
		info.writeTo(new DataOutputStream(output));
		input = new ByteArrayInputStream(output.toByteArray());
		newInfo.readFrom(0, new DataInputStream(input));
		assertEquals(info, newInfo);

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
		output = new ByteArrayOutputStream();
		info.writeTo(new DataOutputStream(output));
		newInfo = new ResourceInfo();
		input = new ByteArrayInputStream(output.toByteArray());
		newInfo.readFrom(0, new DataInputStream(input));
		assertEquals(info, newInfo);
	}

}
