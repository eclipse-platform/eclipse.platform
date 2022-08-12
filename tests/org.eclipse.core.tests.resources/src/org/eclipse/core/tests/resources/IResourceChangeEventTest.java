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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

/**
 * This class tests the public API of IResourceChangeEvent.
 */
public class IResourceChangeEventTest extends ResourceTest {
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
	@Override
	protected void setUp() throws Exception {
		super.setUp();

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
			ensureExistsInWorkspace(allResources, true);
			marker2 = file2.createMarker(IMarker.BOOKMARK);
			marker3 = file3.createMarker(IMarker.BOOKMARK);
		};
		try {
			getWorkspace().run(body, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
	}

	/**
	 * Tears down the fixture, for example, close a network connection.
	 * This method is called after a test is executed.
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		ensureDoesNotExistInWorkspace(getWorkspace().getRoot());
	}

	/**
	 * Tests the IResourceChangeEvent#findMarkerDeltas method.
	 */
	public void testFindMarkerDeltas() {
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
			assertNotNull("10.0", deltas);
			assertTrue("10.1", deltas.length == 0);

			//marker type, with subtypes
			deltas = event.findMarkerDeltas(IMarker.MARKER, true);
			verifyDeltas(deltas);

			//problem type, with subtypes
			deltas = event.findMarkerDeltas(IMarker.PROBLEM, true);
			assertNotNull("12.0", deltas);
			assertTrue("12.1", deltas.length == 0);

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
			getWorkspace().run(body, getMonitor());
		} catch (CoreException e) {
			fail("Exception1", e);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
	}

	public void testFindMarkerDeltasInEmptyDelta() {
		/*
		 * The following changes will occur:
		 * - change file1
		 */
		IResourceChangeListener listener = event -> {
			//bookmark type, no subtypes
			IMarkerDelta[] deltas = event.findMarkerDeltas(IMarker.BOOKMARK, false);
			assertNotNull("1.0", deltas);
			assertTrue("1.1", deltas.length == 0);

			//bookmark type, with subtypes
			deltas = event.findMarkerDeltas(IMarker.BOOKMARK, true);
			assertNotNull("2.0", deltas);
			assertTrue("2.1", deltas.length == 0);

			//marker type, no subtypes
			deltas = event.findMarkerDeltas(IMarker.MARKER, false);
			assertNotNull("3.0", deltas);
			assertTrue("3.1", deltas.length == 0);

			//marker type, with subtypes
			deltas = event.findMarkerDeltas(IMarker.MARKER, true);
			assertNotNull("4.0", deltas);
			assertTrue("4.1", deltas.length == 0);

			//problem type, with subtypes
			deltas = event.findMarkerDeltas(IMarker.PROBLEM, true);
			assertNotNull("5.0", deltas);
			assertTrue("5.1", deltas.length == 0);

			//all types, include subtypes
			deltas = event.findMarkerDeltas(null, true);
			assertNotNull("6.0", deltas);
			assertTrue("6.1", deltas.length == 0);

			//all types, no subtypes
			deltas = event.findMarkerDeltas(null, false);
			assertNotNull("7.0", deltas);
			assertTrue("7.1", deltas.length == 0);
		};
		getWorkspace().addResourceChangeListener(listener);

		//do the work
		try {
			file1.setContents(getRandomContents(), true, true, getMonitor());
		} catch (CoreException e) {
			fail("Exception2", e);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
	}

	/**
	 * Verifies that the marker deltas have the right changes.
	 */
	protected void verifyDeltas(IMarkerDelta[] deltas) {
		assertNotNull("1.0", deltas);
		assertTrue("1.1", deltas.length == 3);
		//delta order is not defined..
		boolean found1 = false, found2 = false, found3 = false;
		for (int i = 0; i < deltas.length; i++) {
			assertTrue("kind" + i, deltas[i].getType().equals(IMarker.BOOKMARK));
			long id = deltas[i].getId();
			if (id == marker1.getId()) {
				found1 = true;
				assertTrue("2.0", deltas[i].getKind() == IResourceDelta.ADDED);
			} else if (id == marker2.getId()) {
				found2 = true;
				assertTrue("3.0", deltas[i].getKind() == IResourceDelta.REMOVED);
			} else if (id == marker3.getId()) {
				found3 = true;
				assertTrue("4.0", deltas[i].getKind() == IResourceDelta.CHANGED);
			} else {
				assertTrue("4.99", false);
			}
		}
		assertTrue("5.0", found1);
		assertTrue("5.1", found2);
		assertTrue("5.2", found3);
	}
}
