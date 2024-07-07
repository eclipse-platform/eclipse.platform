/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.builders;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests the PRE_BUILD and POST_BUILD events.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class BuilderEventTest {
	private BuildEventListener listener;

	@BeforeEach
	public void setUp() throws Exception {
		listener = new BuildEventListener();
		int mask = IResourceChangeEvent.PRE_BUILD | IResourceChangeEvent.POST_BUILD | IResourceChangeEvent.POST_CHANGE;
		getWorkspace().addResourceChangeListener(listener, mask);
	}

	@AfterEach
	public void tearDown() throws Exception {
		getWorkspace().removeResourceChangeListener(listener);
	}

	@Test
	public void testEventsOnClean() throws CoreException {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");
		// Turn auto-building off
		setAutoBuilding(false);
		// Create and open a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// Create and set a build spec for the project
		updateProjectDescription(project).addingCommand(DeltaVerifierBuilder.BUILDER_NAME)
				.withTestBuilderId("Project2Build2").apply();
		listener.reset();
		//start with an incremental build
		getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertEquals(getWorkspace(), listener.getSource());
		assertEquals(IncrementalProjectBuilder.INCREMENTAL_BUILD, listener.getBuildKind());
		assertTrue(listener.hadPreBuild());
		assertTrue(listener.hadPostBuild());
		assertTrue(listener.hadPostChange());

		//do a second incremental build and ensure we still get the events
		listener.reset();
		getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertEquals(getWorkspace(), listener.getSource());
		assertEquals(IncrementalProjectBuilder.INCREMENTAL_BUILD, listener.getBuildKind());
		assertTrue(listener.hadPreBuild());
		assertTrue(listener.hadPostBuild());
		assertTrue(listener.hadPostChange());

		//do a full build and ensure we still get the event
		listener.reset();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(getWorkspace(), listener.getSource());
		assertEquals(IncrementalProjectBuilder.FULL_BUILD, listener.getBuildKind());
		assertTrue(listener.hadPreBuild());
		assertTrue(listener.hadPostBuild());
		assertTrue(listener.hadPostChange());

		//do a clean build and ensure we get the same events
		listener.reset();
		getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, createTestMonitor());
		assertEquals(getWorkspace(), listener.getSource());
		assertEquals(IncrementalProjectBuilder.CLEAN_BUILD, listener.getBuildKind());
		assertTrue(listener.hadPreBuild());
		assertTrue(listener.hadPostBuild());
		assertTrue(listener.hadPostChange());
	}

}
