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
package org.eclipse.core.tests.resources.session;

import junit.framework.Test;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Ensures that the workspace description is correctly persisted across
 * sessions.
 */
public class WorkspaceDescriptionTest extends WorkspaceSessionTest {
	private static final String[] BUILD_ORDER = new String[] {"Foo"};
	private static final boolean APPLY_POLICY = false;
	private static final int STATE_LONGEVITY = 123456;
	private static final int MAX_STATES = 244;
	private static final long MAX_FILE_SIZE = 1024 * 53;
	private static final long SNAPSHOT_INTERVAL = 4321;

	public void test1() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc = workspace.getDescription();
		desc.setAutoBuilding(false);
		desc.setBuildOrder(BUILD_ORDER);
		desc.setApplyFileStatePolicy(APPLY_POLICY);
		desc.setFileStateLongevity(STATE_LONGEVITY);
		desc.setMaxFileStates(MAX_STATES);
		desc.setMaxFileStateSize(MAX_FILE_SIZE);
		desc.setSnapshotInterval(SNAPSHOT_INTERVAL);
		try {
			workspace.setDescription(desc);
			workspace.save(true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
	}

	public void test2() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc = workspace.getDescription();
		assertTrue("2.0", !desc.isAutoBuilding());
		assertEquals("2.1", BUILD_ORDER, desc.getBuildOrder());
		assertEquals("2.2", APPLY_POLICY, desc.isApplyFileStatePolicy());
		assertEquals("2.3", STATE_LONGEVITY, desc.getFileStateLongevity());
		assertEquals("2.4", MAX_STATES, desc.getMaxFileStates());
		assertEquals("2.5", MAX_FILE_SIZE, desc.getMaxFileStateSize());
		assertEquals("2.6", SNAPSHOT_INTERVAL, desc.getSnapshotInterval());
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, WorkspaceDescriptionTest.class);
	}
}
