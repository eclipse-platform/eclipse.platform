/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.core.tests.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * This class tests the public API of IResourceChangeEvent.
 */
public class IResourceChangeEventTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	/* some random resource handles */
	protected IProject project1;
	protected IProject project2;
	protected IFolder folder1;//below project2
	protected IFolder folder2;//below folder1
	protected IFolder folder3;//same as file1
	protected IFile file1;//below folder1
	protected IFile file2;//below folder1
	protected IFile file3;//below folder2
	protected IMarker marker1;//on file1
	protected IMarker marker2;//on file1
	protected IMarker marker3;//on file1

	protected IResource[] allResources;

	/**
	 * Sets up the fixture, for example, open a network connection.
	 * This method is called before a test is executed.
	 */
	@Before
	public void setUp() throws Exception {
		// Create some resource handles
		project1 = getWorkspace().getRoot().getProject("Project" + 1);
		project2 = getWorkspace().getRoot().getProject("Project" + 2);
		folder1 = project1.getFolder("Folder" + 1);
		folder2 = folder1.getFolder("Folder" + 2);
		folder3 = folder1.getFolder("Folder" + 3);
		file1 = folder1.getFile("File" + 1);
		file2 = folder1.getFile("File" + 2);
		file3 = folder2.getFile("File" + 3);
		allResources = new IResource[] {project1, project2, folder1, folder2, folder3, file1, file2, file3};

		// Create and open the resources
		IWorkspaceRunnable body = monitor -> {
			createInWorkspace(allResources);
			marker2 = file2.createMarker(IMarker.BOOKMARK);
			marker3 = file3.createMarker(IMarker.BOOKMARK);
		};
		getWorkspace().run(body, createTestMonitor());
	}

	/**
	 * Tests the IResourceChangeEvent#findMarkerDeltas method.
	 */
	@Test
	public void testFindMarkerDeltas() throws CoreException {
		/*
		 * The following changes will occur:
		 * - add marker1
		 * - remove marker2
		 * - change marker3
		 */
		IResourceChangeListener listener = event -> {
			//bookmark type, no subtypes
			IMarkerDelta[] deltas = event.findMarkerDeltas(IMarker.BOOKMARK, false);
			verifyDeltas(deltas);

			//bookmark type, with subtypes
			deltas = event.findMarkerDeltas(IMarker.BOOKMARK, true);
			verifyDeltas(deltas);

			//marker type, no subtypes
			deltas = event.findMarkerDeltas(IMarker.MARKER, false);
			assertThat(deltas).isEmpty();

			//marker type, with subtypes
			deltas = event.findMarkerDeltas(IMarker.MARKER, true);
			verifyDeltas(deltas);

			//problem type, with subtypes
			deltas = event.findMarkerDeltas(IMarker.PROBLEM, true);
			assertThat(deltas).isEmpty();

			//all types, include subtypes
			deltas = event.findMarkerDeltas(null, true);
			verifyDeltas(deltas);

			//all types, no subtypes
			deltas = event.findMarkerDeltas(null, false);
			verifyDeltas(deltas);
		};
		getWorkspace().addResourceChangeListener(listener);

		//do the work
		IWorkspaceRunnable body = monitor -> {
			marker1 = file1.createMarker(IMarker.BOOKMARK);
			marker2.delete();
			marker3.setAttribute("Foo", true);
		};
		try {
			getWorkspace().run(body, createTestMonitor());
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
	}

	@Test
	public void testFindMarkerDeltasInEmptyDelta() throws CoreException {
		/*
		 * The following changes will occur:
		 * - change file1
		 */
		IResourceChangeListener listener = event -> {
			//bookmark type, no subtypes
			assertThat(event.findMarkerDeltas(IMarker.BOOKMARK, false)).isEmpty();

			//bookmark type, with subtypes
			assertThat(event.findMarkerDeltas(IMarker.BOOKMARK, true)).isEmpty();

			//marker type, no subtypes
			assertThat(event.findMarkerDeltas(IMarker.MARKER, false)).isEmpty();

			//marker type, with subtypes
			assertThat(event.findMarkerDeltas(IMarker.MARKER, true)).isEmpty();

			//problem type, with subtypes
			assertThat(event.findMarkerDeltas(IMarker.PROBLEM, true)).isEmpty();

			//all types, include subtypes
			assertThat(event.findMarkerDeltas(null, true)).isEmpty();

			//all types, no subtypes
			assertThat(event.findMarkerDeltas(null, false)).isEmpty();
		};
		getWorkspace().addResourceChangeListener(listener);

		//do the work
		try {
			file1.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
	}

	/**
	 * Verifies that the marker deltas have the right changes.
	 */
	protected void verifyDeltas(IMarkerDelta[] deltas) {
		Map<Long, Integer> deltaIdToKind = Map.of(marker1.getId(), IResourceDelta.ADDED, //
				marker2.getId(), IResourceDelta.REMOVED, //
				marker3.getId(), IResourceDelta.CHANGED);
		assertThat(deltas).hasSize(3).allSatisfy(delta -> {
			assertThat(delta.getType()).as("type").isEqualTo(IMarker.BOOKMARK);
			assertThat(delta.getKind()).as("kind").isNotNull().isEqualTo(deltaIdToKind.get(delta.getId()));
		}).anySatisfy(delta -> assertThat(delta.getKind()).isEqualTo(IResourceDelta.ADDED))
				.anySatisfy(delta -> assertThat(delta.getKind()).isEqualTo(IResourceDelta.REMOVED))
				.anySatisfy(delta -> assertThat(delta.getKind()).isEqualTo(IResourceDelta.CHANGED));
	}

}
