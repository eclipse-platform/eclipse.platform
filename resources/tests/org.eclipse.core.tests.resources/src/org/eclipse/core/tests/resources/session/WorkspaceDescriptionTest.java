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

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import junit.framework.Test;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Ensures that the workspace description is correctly persisted across
 * sessions.
 */
public class WorkspaceDescriptionTest extends WorkspaceSessionTest {
	private static final String[] BUILD_ORDER = new String[] {"Foo"};
	private static final boolean APPLY_POLICY = false;
	private static final long STATE_LONGEVITY = 123456;
	private static final int MAX_STATES = 244;
	private static final long MAX_FILE_SIZE = 1024 * 53;
	private static final long SNAPSHOT_INTERVAL = 4321;

	public void test1() throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc = workspace.getDescription();
		desc.setAutoBuilding(false);
		desc.setBuildOrder(BUILD_ORDER);
		desc.setApplyFileStatePolicy(APPLY_POLICY);
		desc.setFileStateLongevity(STATE_LONGEVITY);
		desc.setMaxFileStates(MAX_STATES);
		desc.setMaxFileStateSize(MAX_FILE_SIZE);
		desc.setSnapshotInterval(SNAPSHOT_INTERVAL);
		workspace.setDescription(desc);
		workspace.save(true, createTestMonitor());
	}

	public void test2() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc = workspace.getDescription();
		assertThat(desc).matches(not(IWorkspaceDescription::isAutoBuilding), "auto building is disabled");
		assertThat(desc.getBuildOrder()).as("buildorder in: %s", desc).isEqualTo(BUILD_ORDER);
		assertThat(desc.isApplyFileStatePolicy()).as("apply file state policy in: %s", desc)
				.isEqualTo(APPLY_POLICY);
		assertThat(desc.getFileStateLongevity()).as("longevity in: %s", desc).isEqualTo(STATE_LONGEVITY);
		assertThat(desc.getMaxFileStates()).as("max states in: %s", desc).isEqualTo(MAX_STATES);
		assertThat(desc.getMaxFileStateSize()).as("max file size in: %s", desc).isEqualTo(MAX_FILE_SIZE);
		assertThat(desc.getSnapshotInterval()).as("snapshot interval in: %s", desc)
				.isEqualTo(SNAPSHOT_INTERVAL);
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, WorkspaceDescriptionTest.class);
	}
}
