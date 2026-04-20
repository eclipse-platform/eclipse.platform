/*******************************************************************************
 * Copyright (c) 2026 Vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests the PRE_PROJECT_BUILD and POST_PROJECT_BUILD events. In particular it
 * asserts that events fire even when a builder is short-circuited by the
 * needsBuild check (e.g. an incremental build after Project > Clean where no
 * source changed).
 */
@ExtendWith(WorkspaceResetExtension.class)
public class ProjectBuildEventTest {

	private record Record(int type, Object source, String builderName, int buildKind) {
	}

	private final List<Record> events = new CopyOnWriteArrayList<>();
	private final IResourceChangeListener listener = event -> events.add(
			new Record(event.getType(), event.getSource(), event.getBuilderName(), event.getBuildKind()));

	@BeforeEach
	public void setUp() {
		int mask = IResourceChangeEvent.PRE_BUILD | IResourceChangeEvent.POST_BUILD
				| IResourceChangeEvent.PRE_PROJECT_BUILD | IResourceChangeEvent.POST_PROJECT_BUILD;
		getWorkspace().addResourceChangeListener(listener, mask);
	}

	@AfterEach
	public void tearDown() {
		getWorkspace().removeResourceChangeListener(listener);
	}

	@Test
	public void testPerBuilderEventsFireOnFullBuild() throws CoreException {
		IProject project = createProjectWithBuilder("FULL");

		events.clear();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		List<Record> perBuilder = filter(IResourceChangeEvent.PRE_PROJECT_BUILD,
				IResourceChangeEvent.POST_PROJECT_BUILD);
		assertEquals(2, perBuilder.size(), "expected one PRE/POST_PROJECT_BUILD pair, got events: " + events);

		Record pre = perBuilder.get(0);
		Record post = perBuilder.get(1);
		assertEquals(IResourceChangeEvent.PRE_PROJECT_BUILD, pre.type());
		assertEquals(IResourceChangeEvent.POST_PROJECT_BUILD, post.type());
		assertEquals(project, pre.source());
		assertEquals(project, post.source());
		assertEquals(DeltaVerifierBuilder.BUILDER_NAME, pre.builderName());
		assertEquals(DeltaVerifierBuilder.BUILDER_NAME, post.builderName());
		assertEquals(IncrementalProjectBuilder.FULL_BUILD, pre.buildKind());
		assertEquals(IncrementalProjectBuilder.FULL_BUILD, post.buildKind());
	}

	@Test
	public void testPerBuilderEventsFireOnCleanBuild() throws CoreException {
		createProjectWithBuilder("CLEAN");
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		events.clear();
		getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, createTestMonitor());
		List<Record> perBuilder = filter(IResourceChangeEvent.PRE_PROJECT_BUILD,
				IResourceChangeEvent.POST_PROJECT_BUILD);
		assertEquals(2, perBuilder.size(), "CLEAN should fire one PRE/POST_PROJECT_BUILD pair, got: " + events);
		assertEquals(IncrementalProjectBuilder.CLEAN_BUILD, perBuilder.get(0).buildKind());
		assertEquals(IncrementalProjectBuilder.CLEAN_BUILD, perBuilder.get(1).buildKind());
	}

	@Test
	public void testPerBuilderEventsFireOnIncrementalBuildWithNoDelta() throws CoreException {
		createProjectWithBuilder("INC-NO-DELTA");
		// Prime: run a full build so the builder has a last-built tree.
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		// Second incremental build without any intervening source change. The
		// builder should be considered but short-circuited by needsBuild. Events
		// must still fire so the Build Monitor view can render the session.
		events.clear();
		getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		List<Record> perBuilder = filter(IResourceChangeEvent.PRE_PROJECT_BUILD,
				IResourceChangeEvent.POST_PROJECT_BUILD);
		assertEquals(2, perBuilder.size(),
				"INCREMENTAL with no delta must still fire PRE/POST_PROJECT_BUILD, got: " + events);
	}

	@Test
	public void testPerBuilderEventsNestedInsideWorkspaceBuild() throws CoreException {
		createProjectWithBuilder("NESTED");

		events.clear();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		List<Integer> types = new ArrayList<>();
		for (Record r : events) {
			types.add(r.type());
		}
		int preBuild = types.indexOf(IResourceChangeEvent.PRE_BUILD);
		int postBuild = types.indexOf(IResourceChangeEvent.POST_BUILD);
		int prePrj = types.indexOf(IResourceChangeEvent.PRE_PROJECT_BUILD);
		int postPrj = types.indexOf(IResourceChangeEvent.POST_PROJECT_BUILD);
		assertTrue(preBuild >= 0 && postBuild > preBuild, "workspace PRE/POST_BUILD must be present and ordered: " + events);
		assertTrue(prePrj > preBuild && prePrj < postBuild,
				"PRE_PROJECT_BUILD must be nested inside workspace build: " + events);
		assertTrue(postPrj > prePrj && postPrj < postBuild,
				"POST_PROJECT_BUILD must come after its PRE and before workspace POST_BUILD: " + events);
	}

	@Test
	public void testBuilderNameOnlyPopulatedForProjectBuildEvents() throws CoreException {
		createProjectWithBuilder("NAME");
		events.clear();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		boolean sawProjectBuildWithName = false;
		for (Record r : events) {
			if (r.type() == IResourceChangeEvent.PRE_PROJECT_BUILD
					|| r.type() == IResourceChangeEvent.POST_PROJECT_BUILD) {
				assertNotNull(r.builderName(), "per-builder event must carry builder name");
				sawProjectBuildWithName = true;
			} else {
				assertNull(r.builderName(), "workspace-level event must not carry builder name, got: " + r);
			}
		}
		assertTrue(sawProjectBuildWithName, "expected at least one per-builder event: " + events);
	}

	private IProject createProjectWithBuilder(String tag) throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("PROJECT-" + tag);
		setAutoBuilding(false);
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		updateProjectDescription(project).addingCommand(DeltaVerifierBuilder.BUILDER_NAME)
				.withTestBuilderId("Builder-" + tag).apply();
		return project;
	}

	private List<Record> filter(int... types) {
		List<Record> out = new ArrayList<>();
		for (Record r : events) {
			for (int t : types) {
				if (r.type() == t) {
					out.add(r);
					break;
				}
			}
		}
		return out;
	}
}
