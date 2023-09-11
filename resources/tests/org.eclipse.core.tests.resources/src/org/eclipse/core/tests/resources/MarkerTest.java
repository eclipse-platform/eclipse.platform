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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.eclipse.core.internal.resources.MarkerManager;
import org.eclipse.core.internal.resources.MarkerReader;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.watson.IPathRequestor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

public class MarkerTest extends ResourceTest {

	public static final String TRANSIENT_MARKER = "org.eclipse.core.tests.resources.transientmarker";
	public static final String TEST_PROBLEM_MARKER = "org.eclipse.core.tests.resources.testproblem";

	/** The collection of resources used for testing. */
	IResource[] resources;
	private boolean originalRefreshSetting;

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 */
	public void testMarkerChangesInDelta3() {
		debug("TestMarkerChangesInDelta3");

		// Create and register a listener.
		final MarkersChangeListener listener = new MarkersChangeListener();
		addResourceChangeListener(listener);

		try {
			IResource resource = getWorkspace().getRoot().findMember("1");
			IResource destinationResource = null;
			IResource child;
			IResource destinationChild;
			IPath destination;
			IMarker[] markers = new IMarker[4];

			// MOVE the resource
			try {
				destination = resource.getFullPath().removeLastSegments(1).append(resource.getFullPath().lastSegment() + "new");
				markers[0] = resource.createMarker(IMarker.BOOKMARK);
				child = ((IProject) resource).members()[0];
				markers[1] = child.createMarker(IMarker.TASK);
				listener.reset();
				resource.move(destination, false, getMonitor());
				destinationResource = getWorkspace().getRoot().findMember(destination);
				markers[2] = destinationResource.getMarker(markers[0].getId());
				destinationChild = ((IProject) destinationResource).findMember(child.getName());
				markers[3] = destinationChild.getMarker(markers[1].getId());
				assertEquals("1.1." + resource.getFullPath(), 4, listener.numAffectedResources());
				assertTrue("1.2." + resource.getFullPath(), listener.checkChanges(resource, null, new IMarker[] {markers[0]}, null));
				assertTrue("1.3." + resource.getFullPath(), listener.checkChanges(child, null, new IMarker[] {markers[1]}, null));
				assertTrue("1.4." + destinationResource.getFullPath(), listener.checkChanges(destinationResource, new IMarker[] {markers[2]}, null, null));
				assertTrue("1.5." + destinationResource.getFullPath(), listener.checkChanges(destinationChild, new IMarker[] {markers[3]}, null, null));
			} catch (CoreException e) {
				fail("1.99", e);
			}

			// COPY the resource and look at the deltas -
			// there should be no changes since markers are not copied
			try {
				resource = getWorkspace().getRoot().findMember("2");
				destination = resource.getFullPath().removeLastSegments(1).append(resource.getFullPath().lastSegment() + "copy");
				resource.createMarker(IMarker.BOOKMARK);
				listener.reset();
				resource.copy(destination, false, getMonitor());
				assertEquals("2.1." + resource.getFullPath(), 0, listener.numAffectedResources());
			} catch (CoreException e) {
				fail("2.99", e);
			}

			// delete all markers for a clean run next time
			try {
				getWorkspace().getRoot().deleteMarkers(null, true, IResource.DEPTH_INFINITE);
			} catch (CoreException e) {
				fail("99.99", e);
			}
		} finally {
			removeResourceChangeListener(listener);
		}
	}

	private void removeResourceChangeListener(final MarkersChangeListener listener) {
		// removeResourceChangeListener need to happen in an atomic workspace operation
		// otherwise it would be removed while auto refresh is running
		// and might even get called in another thread after removing in this thread
		try {
			// listener.shutDown();
			getWorkspace().run(p -> getWorkspace().removeResourceChangeListener(listener), null);
		} catch (CoreException e) {
			fail("removeResourceChangeListener", e);
		}
	}

	private void addResourceChangeListener(MarkersChangeListener listener) {
		// addResourceChangeListener need to happen in an atomic workspace operation
		// otherwise it would be added while auto refresh is running
		// and might get called in another thread before explicit refresh in this thread
		try {
			getWorkspace().run(p -> {
				getWorkspace().addResourceChangeListener(listener);
				// listener.active();
			}, null);
		} catch (CoreException e) {
			fail("removeResourceChangeListener", e);
		}
	}

	protected void addChildren(ArrayList<String> result, IPath root, int breadth, int depth) {
		for (int i = 1; i < breadth + 1; i++) {
			IPath child = root.append(i + "");
			if (depth == 0) {
				result.add(child.toString());
				return;
			}
			child = child.addTrailingSeparator();
			result.add(child.toString());
			addChildren(result, child, breadth, depth - 1);
		}
	}

	protected void assertDoesNotExist(String message, IMarker[] markers) {
		for (IMarker marker : markers) {
			assertDoesNotExist(message, marker);
		}
	}

	protected void assertDoesNotExist(String message, IMarker marker) {
		assertTrue(message, !marker.exists());
	}

	/**
	 * Asserts that the given collection of expected markers contains
	 * the same markers as the given collection of actual markers.  The
	 * markers do not have to be in the same order.
	 */
	protected void assertEquals(String message, IMarker[] expectedMarkers, IMarker[] actualMarkers) {
		int n = expectedMarkers.length;
		if (n != actualMarkers.length) {
			fail(message);
		}
		boolean[] seen = new boolean[n];
		for (int i = 0; i < n; ++i) {
			boolean found = false;
			for (int j = 0; j < n; ++j) {
				if (!seen[j] && equals(expectedMarkers[i], actualMarkers[j])) {
					found = true;
					seen[j] = true;
					break;
				}
			}
			if (!found) {
				fail(message);
			}
		}
	}

	protected void assertEquals(String message, Map<String, ? extends Object> map, Object[] keys, Object[] values) {
		assertEquals(message, keys.length, values.length);
		assertEquals(message, keys.length, map.size());
		for (Map.Entry<String, ? extends Object> entry : map.entrySet()) {
			Object key = entry.getKey();
			Object value = entry.getValue();
			boolean found = false;
			for (int j = 0; !found && j < keys.length; j++) {
				if (keys[j].equals(key)) {
					found = true;
					if (!values[j].equals(value)) {
						fail(message);
					}
				}
			}
			if (!found) {
				assertTrue(message, false);
			}
		}
	}

	protected void assertExists(String message, IMarker[] markers) {
		for (IMarker marker : markers) {
			assertExists(message, marker);
		}
	}

	protected void assertExists(String message, List<IMarker> markers) {
		for (IMarker marker : markers) {
			assertExists(message, marker);
		}
	}


	protected void assertExists(String message, IMarker marker) {
		assertTrue(message, marker.exists());
	}

	public IResource[] createLargeHierarchy() {
		ArrayList<String> result = new ArrayList<>();
		result.add("/");
		new MarkerTest().addChildren(result, IPath.ROOT, 3, 4);
		String[] names = result.toArray(new String[result.size()]);
		IResource[] created = buildResources(getWorkspace().getRoot(), names);
		ensureExistsInWorkspace(created, true);
		return created;
	}

	protected IMarker[] createMarkers(final IResource[] hosts, final String type) throws CoreException {
		final IMarker[] result = new IMarker[hosts.length];
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			for (int i = 0; i < hosts.length; i++) {
				result[i] = hosts[i].createMarker(type);
			}
		}, getMonitor());
		return result;
	}

	public void createProblem(IResource host, int severity) {
		try {
			IMarker marker = host.createMarker(IMarker.PROBLEM);
			marker.setAttribute(IMarker.SEVERITY, severity);
		} catch (CoreException e) {
			fail("Failed to create problem on resource: " + host, e);
		}
	}

	/**
	 * Return a string array which defines the hierarchy of a tree.
	 * Folder resources must have a trailing slash.
	 */
	@Override
	public String[] defineHierarchy() {
		return new String[] {"/", "1/", "1/1", "1/2/", "1/2/1", "1/2/2/", "2/", "2/1", "2/2/", "2/2/1", "2/2/2/"};
	}

	public void display(long start, long stop) {
		display("Start: " + start);
		display("Stop: " + stop);
		display("Duration: " + (stop - start));
	}

	public void display(String message) {
		System.out.println(message);
	}

	/**
	 * Returns whether the given markers are equal.
	 */
	public static boolean equals(IMarker a, IMarker b) {
		try {
			if (a.getType() != b.getType()) {
				return false;
			}
			if (a.getId() != b.getId()) {
				return false;
			}
			return true;
		} catch (CoreException e) {
			return false;
		}
	}

	/**
	 * Returns whether two object are both null, or both non-null and equal.
	 */
	protected static boolean equalsOrNull(Object a, Object b) {
		return a == b || (a != null && b != null && a.equals(b));
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		resources = createHierarchy();

		// disable autorefresh an wait till that is finished
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		originalRefreshSetting = prefs.getBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, false);
		prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, false);
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, null);
	}

	@Override
	public void tearDown() throws Exception {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, originalRefreshSetting);
		super.tearDown();
	}

	/**
	 * Tests whether markers correctly copy with resources.
	 */
	public void testCopyResource() {
		debug("TestCopyResource");
	}

	public void testCreateMarker() {
		debug("TestCreateMarker");

		// create markers on our hierarchy of resources
		for (IResource resource : resources) {
			// Create and register a listener.
			MarkersChangeListener listener = new MarkersChangeListener();
			addResourceChangeListener(listener);
			try {
				IMarker[] markers = new IMarker[3];
				try {
					markers[0] = resource.createMarker(IMarker.PROBLEM);
					markers[1] = resource.createMarker(IMarker.BOOKMARK);
					markers[2] = resource.createMarker(IMarker.TASK);
					assertExists("1.0." + resource.getFullPath(), markers);
				} catch (CoreException e) {
					fail("1.1." + resource.getFullPath(), e);
				}
				assertEquals("1.2." + resource.getFullPath(), 1, listener.numAffectedResources());
				assertTrue("1.3." + resource.getFullPath(), listener.checkChanges(resource, markers, null, null));

				// expect an AssertionFailedException from a null type
				try {
					resource.createMarker(null);
					fail("2.0." + resource.getFullPath());
				} catch (CoreException e) {
					fail("2.1." + resource.getFullPath(), e);
				} catch (RuntimeException e) {
					// expected
				}
			} finally {
				removeResourceChangeListener(listener);
			}
		}

		// try creating a marker on a resource which does't exist
		IResource testResource = getWorkspace().getRoot().getFile(IPath.fromOSString("non/existant/resource"));
		assertTrue("3.0", !testResource.exists());
		try {
			testResource.createMarker(IMarker.PROBLEM);
			fail("3.1");
		} catch (CoreException e) {
			// expected
		}
	}

	public void testCreateMarkerWithAttributes() {
		// Create and register a listener.
		MarkersChangeListener listener = new MarkersChangeListener();
		addResourceChangeListener(listener);

		// create markers on our hierarchy of resources
		for (IResource resource : resources) {
			listener.reset();

			List<IMarker> markers = new ArrayList<>();
			try {
				markers.add(resource.createMarker(IMarker.PROBLEM, null));
				markers.add(resource.createMarker(IMarker.BOOKMARK, Collections.emptyMap()));
				markers.add(resource.createMarker(IMarker.TASK, Map.of(IMarker.MESSAGE, "My text")));
				assertExists("Markers do not exist" + resource.getFullPath(), markers);
			} catch (CoreException e) {
				fail("Failed to created markers for resource" + resource.getFullPath(), e);
			}
			assertEquals("Not exactly one resource affected by the marker cahnge" + resource.getFullPath(), 1,
					listener.numAffectedResources());
			assertTrue("Not exactly the markers created as defined" + resource.getFullPath(),
					listener.checkChanges(resource, markers.toArray(new IMarker[0]), null, null));

		}

		// cleanup
		removeResourceChangeListener(listener);
	}

	public void testCreateNullMarkerWithAttributesShouldFail() {
		// create markers on our hierarchy of resources
		for (IResource resource : resources) {
			try {
				resource.createMarker(null, null);
				fail("Creating a null marker should not work" + resource.getFullPath());
			} catch (CoreException e) {
				fail("Core exception during the creation of a null marker" + resource.getFullPath(), e);
			} catch (RuntimeException e) {
				// expected
			}
		}

	}

	public void testCreateMarkerWithAttributesOnAResourceWhichDoesNotExistShouldFail() {

		// try creating a marker on a resource which does't exist
		IResource testResource = getWorkspace().getRoot().getFile(IPath.fromOSString("non/existant/resource"));
		assertTrue("Resource should not exist", !testResource.exists());
		try {
			testResource.createMarker(IMarker.PROBLEM, Map.of(IMarker.MESSAGE, "My text"));
			fail("Creating a marker for a non existing resource should not work");
		} catch (CoreException e) {
			// expected
		}
	}

	// testing that markers creation and calling setAttribute trigger multiple
	// resource change
	// events (which is bad for performance hence the better createMarker(String
	// type, Map<String, Object> attributes) method

	public void testThatSettingAttributesTriggerAdditionalResourceChangeEvent() {
		// Create and register a listener.
		MarkersNumberOfDeltasChangeListener listener = new MarkersNumberOfDeltasChangeListener();
		getWorkspace().addResourceChangeListener(listener);
		for (IResource resource : resources) {
			listener.reset();
			// each setAttributes triggers one additional resource change event
			try {
				IMarker marker = resource.createMarker(TEST_PROBLEM_MARKER);
				marker.setAttribute(IMarker.MESSAGE, getRandomString());
				marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
				assertEquals(3, listener.numberOfChanges());
			} catch (CoreException e) {
				fail("Marker creation failed unexpected", e);
			}
		}
		// cleanup
		getWorkspace().removeResourceChangeListener(listener);
	}

	// testing that markers creation with arbiutes
	public void testThatMarkersWithAttributesOnlyTriggerOnResourceChangeEvent() {
		// Create and register a listener.
		MarkersNumberOfDeltasChangeListener listener = new MarkersNumberOfDeltasChangeListener();
		getWorkspace().addResourceChangeListener(listener);
		for (IResource resource : resources) {
			listener.reset();
			// createMarker with attributes triggers only one resource change event
			listener.reset();
			// each setAttributes triggers one resource change event
			try {
				resource.createMarker(TEST_PROBLEM_MARKER,
						Map.of(IMarker.MESSAGE, getRandomString(), IMarker.PRIORITY, IMarker.PRIORITY_HIGH));
				assertEquals(1, listener.numberOfChanges());
			} catch (CoreException e) {
				fail("Marker creation failed unexpected", e);
			}
		}
		// cleanup
		getWorkspace().removeResourceChangeListener(listener);
	}
	public void testCreationTime() {

		for (int i = 0; i < resources.length; i++) {
			IMarker marker = null;
			try {
				marker = resources[i].createMarker(IMarker.PROBLEM);
			} catch (CoreException e) {
				fail("0.0", e);
			}

			// make sure the marker has a non-zero creation time
			try {
				assertTrue("1.0." + i, 0 != marker.getCreationTime());
			} catch (CoreException e) {
				fail("1.1", e);
			}
		}
	}

	public void testDeleteMarker() {
		debug("TestDeleteMarker");
		IMarker marker = null;

		// Create and register a listener.
		MarkersChangeListener listener = new MarkersChangeListener();
		addResourceChangeListener(listener);

		// for each resource in the hierarchy do...
		for (IResource resource : resources) {
			// create the marker, assert that it exists, and then remove it
			try {
				listener.reset();
				marker = resource.createMarker(IMarker.PROBLEM);
				assertEquals("2.0." + resource.getFullPath(), 1, listener.numAffectedResources());
				assertTrue("2.1." + resource.getFullPath(), listener.checkChanges(resource, new IMarker[] {marker}, null, null));
			} catch (CoreException e) {
				fail("2.2." + resource.getFullPath(), e);
			}
			assertExists("2.3." + resource.getFullPath(), marker);
			try {
				listener.reset();
				marker.delete();
				assertDoesNotExist("2.4." + resource.getFullPath(), marker);
				assertEquals("2.5." + resource.getFullPath(), 1, listener.numAffectedResources());
				assertTrue("2.6." + resource.getFullPath(), listener.checkChanges(resource, null, new IMarker[] {marker}, null));
			} catch (CoreException e) {
				fail("2.7." + resource.getFullPath(), e);
			}

			// Check that a non-existant marker can be removed
			try {
				marker.delete();
			} catch (CoreException e) {
				fail("3.0." + resource.getFullPath(), e);
			}

			// create multiple markers, assert they exist, and then remove them.
			IMarker[] markers = new IMarker[3];
			try {
				markers[0] = resource.createMarker(IMarker.BOOKMARK);
				assertExists("4.0." + resource.getFullPath(), markers[0]);
				markers[1] = resource.createMarker(IMarker.TASK);
				assertExists("4.1." + resource.getFullPath(), markers[1]);
				markers[2] = resource.createMarker(IMarker.PROBLEM);
				assertExists("4.2." + resource.getFullPath(), markers[2]);
			} catch (CoreException e) {
				fail("4.3." + resource.getFullPath(), e);
			}
			try {
				listener.reset();
				getWorkspace().deleteMarkers(markers);
				assertEquals("4.4." + resource.getFullPath(), 1, listener.numAffectedResources());
				assertTrue("4.5." + resource.getFullPath(), listener.checkChanges(resource, null, markers, null));
			} catch (CoreException e) {
				fail("4.6." + resource.getFullPath(), e);
			}
			assertDoesNotExist("4.7." + resource.getFullPath(), markers);
		}

		// cleanup
		removeResourceChangeListener(listener);
	}

	public void testDeleteMarkers() {
		debug("TestDeleteMarkers");
		IMarker[] markers = null;
		try {
			markers = createMarkers(resources, IMarker.PROBLEM);
		} catch (CoreException e) {
			fail("0.0", e);
		}

		// Check that a collection of markers can be removed.
		try {
			getWorkspace().deleteMarkers(markers);
		} catch (CoreException e) {
			fail("1.0", e);
		}
		for (IMarker marker : markers) {
			assertTrue("1.1", !marker.exists());
		}

		// Check that an empty collection of markers can be removed.
		try {
			getWorkspace().deleteMarkers(new IMarker[0]);
		} catch (CoreException e) {
			fail("1.2", e);
		}
	}

	public void testFindMarkers() {
		debug("TestFindMarkers");

		// test finding some markers which actually exist
		IMarker[] markers = null;
		try {
			markers = createMarkers(resources, IMarker.PROBLEM);
		} catch (CoreException e) {
			fail("0.0", e);
		}
		try {
			IMarker[] found = getWorkspace().getRoot().findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_INFINITE);
			assertEquals("1.0", markers, found);
		} catch (CoreException e) {
			fail("1.1", e);
		}
		try {
			IMarker[] found = getWorkspace().getRoot().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			assertEquals("1.2", markers, found);
		} catch (CoreException e) {
			fail("1.3", e);
		}

		// test finding some markers which don't exist
		try {
			IMarker[] found = getWorkspace().getRoot().findMarkers(IMarker.BOOKMARK, false, IResource.DEPTH_INFINITE);
			assertTrue("2.0", found.length == 0);
		} catch (CoreException e) {
			fail("2.1", e);
		}

		// add more markers and do a search on all marker types
		Vector<IMarker> allMarkers = new Vector<>(markers.length * 3);
		Collections.addAll(allMarkers, markers);
		try {
			markers = createMarkers(resources, IMarker.BOOKMARK);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		Collections.addAll(allMarkers, markers);
		try {
			markers = createMarkers(resources, IMarker.TASK);
		} catch (CoreException e) {
			fail("3.1", e);
		}
		Collections.addAll(allMarkers, markers);
		try {
			IMarker[] found = getWorkspace().getRoot().findMarkers(null, false, IResource.DEPTH_INFINITE);
			assertEquals("3.2", allMarkers.toArray(new IMarker[allMarkers.size()]), found);
		} catch (CoreException e) {
			fail("3.3", e);
		}
		try {
			IMarker[] found = getWorkspace().getRoot().findMarkers(IMarker.MARKER, true, IResource.DEPTH_INFINITE);
			assertEquals("3.4", allMarkers.toArray(new IMarker[allMarkers.size()]), found);
		} catch (CoreException e) {
			fail("3.5", e);
		}
	}

	/*
	 * Bug 35300 - ClassCastException if marker transient attribute is set to a non-boolean
	 */
	public void test_35300() {
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);
		String MARKER_ID = "foomarker.example.com";
		IMarker marker = null;
		int expected = 4;

		// setup
		try {
			marker = project.createMarker(MARKER_ID);
			marker.setAttribute(IMarker.TRANSIENT, expected);
		} catch (CoreException e) {
			fail("1.0", e);
		} catch (Exception e) {
			fail("1.1", e);
		}

		try {
			int actual = marker.getAttribute(IMarker.TRANSIENT, -1);
			assertEquals("2.1", expected, actual);
			try {
				marker.setAttribute(IMarker.MESSAGE, getRandomString());
			} catch (CoreException e) {
				fail("2.2", e);
			}
		} catch (Exception e) {
			fail("2.3", e);
		}
	}

	public void test_10989() {
		debug("test_10989");

		try {
			IProject project = getWorkspace().getRoot().getProject("MyProject");
			create(project, false);
			IFile file = project.getFile("foo.txt");
			file.create(getRandomContents(), true, null);
			file.createMarker(IMarker.PROBLEM);
			IMarker[] found = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
			assertEquals("1.0", 1, found.length);
			found = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			assertEquals("1.1", 1, found.length);
			project.delete(true, true, null);
		} catch (CoreException e) {
			fail("1.99", e);
		}
	}

	/*
	 * Bug 289811 - ArrayIndexOutOfBoundsException in MarkerAttributeMap
	 */
	public void test_289811() {
		debug("test_289811");

		IFile file = null;
		String testValue = getRandomString();
		try {
			IProject project = getWorkspace().getRoot().getProject(getUniqueString());
			project.create(null);
			project.open(null);
			file = project.getFile("foo.txt");
			file.create(getRandomContents(), true, null);
			IMarker marker = file.createMarker(IMarker.PROBLEM);
			marker.setAttributes(new HashMap<>());
			marker.setAttribute(IMarker.SEVERITY, testValue);
			Object value = marker.getAttribute(IMarker.SEVERITY);
			assertEquals("1.0." + file.getFullPath(), value, testValue);
			project.delete(true, true, null);
		} catch (CoreException e) {
			fail("1.1." + file.getFullPath(), e);
		} catch (ArrayIndexOutOfBoundsException e) {
			fail("1.2." + file.getFullPath(), e);
		} catch (RuntimeException e) {
			fail("1.3." + file.getFullPath(), e);
		}
	}

	/**
	 * Tests public API method IResource#findMaxProblemSeverity
	 */
	public void testFindMaxProblemSeverity() throws CoreException {
		final IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("testFindMaxProblemSeverity");
		IFolder folder = project.getFolder("top");
		IFolder sub = folder.getFolder("sub");
		IFile topFile = folder.getFile("a.txt");
		IFile subFile = sub.getFile("b.txt");
		IResource[] allResources = new IResource[] {project, folder, sub, topFile, subFile};
		ensureExistsInWorkspace(allResources, true);

		assertEquals("1.0", -1, root.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE));
		assertEquals("1.1", -1, root.findMaxProblemSeverity(IMarker.TASK, true, IResource.DEPTH_INFINITE));
		assertEquals("1.2", -1, root.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ZERO));

		createProblem(subFile, IMarker.SEVERITY_INFO);
		assertEquals("2.0", IMarker.SEVERITY_INFO, root.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE));
		assertEquals("2.1", -1, root.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ONE));
		assertEquals("2.2", -1, root.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ZERO));
		assertEquals("2.3", -1, root.findMaxProblemSeverity(IMarker.TASK, true, IResource.DEPTH_INFINITE));

		assertEquals("3.0", IMarker.SEVERITY_INFO, folder.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE));
		assertEquals("3.1", -1, folder.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ONE));
		assertEquals("3.2", -1, folder.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ZERO));
		assertEquals("3.3", -1, folder.findMaxProblemSeverity(IMarker.TASK, true, IResource.DEPTH_INFINITE));

		assertEquals("4.1", IMarker.SEVERITY_INFO, sub.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE));
		assertEquals("4.2", IMarker.SEVERITY_INFO, sub.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ONE));
		assertEquals("4.3", -1, sub.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ZERO));
		assertEquals("4.4", -1, sub.findMaxProblemSeverity(IMarker.TASK, true, IResource.DEPTH_INFINITE));

		assertEquals("5.1", IMarker.SEVERITY_INFO, subFile.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE));
		assertEquals("5.2", IMarker.SEVERITY_INFO, subFile.findMaxProblemSeverity(IMarker.PROBLEM, false, IResource.DEPTH_ONE));
		assertEquals("5.3", IMarker.SEVERITY_INFO, subFile.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ZERO));
		assertEquals("5.4", -1, subFile.findMaxProblemSeverity(IMarker.TASK, true, IResource.DEPTH_INFINITE));

		createProblem(topFile, IMarker.SEVERITY_ERROR);
		assertEquals("6.1", IMarker.SEVERITY_ERROR, root.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE));
		assertEquals("6.2", IMarker.SEVERITY_ERROR, folder.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ONE));
		assertEquals("6.3", IMarker.SEVERITY_ERROR, topFile.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ONE));
		assertEquals("6.4", IMarker.SEVERITY_INFO, sub.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ONE));
		assertEquals("6.5", IMarker.SEVERITY_INFO, subFile.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ZERO));

	}

	/**
	 * Tests public API method IMarker#isSubTypeOf
	 */
	public void testIsSubTypeOf() {
		IProject project = getWorkspace().getRoot().getProject("testisSubType");
		IMarker marker, task, problem, testProblem, invalid;

		final String INVALID_MARKER = "does.not.exist.at.AllMarker";

		try {
			ensureExistsInWorkspace(project, true);
			marker = project.createMarker(IMarker.MARKER);
			task = project.createMarker(IMarker.TASK);
			problem = project.createMarker(IMarker.PROBLEM);
			testProblem = project.createMarker(TEST_PROBLEM_MARKER);
			invalid = project.createMarker(INVALID_MARKER);

			assertTrue("1.0", marker.isSubtypeOf(IMarker.MARKER));
			assertTrue("1.1", !marker.isSubtypeOf(IMarker.TASK));
			assertTrue("1.2", !marker.isSubtypeOf(IMarker.PROBLEM));
			assertTrue("1.3", !marker.isSubtypeOf(TEST_PROBLEM_MARKER));
			assertTrue("1.4", !marker.isSubtypeOf(INVALID_MARKER));

			assertTrue("2.0", task.isSubtypeOf(IMarker.MARKER));
			assertTrue("2.1", task.isSubtypeOf(IMarker.TASK));
			assertTrue("2.2", !task.isSubtypeOf(IMarker.PROBLEM));
			assertTrue("2.3", !task.isSubtypeOf(TEST_PROBLEM_MARKER));
			assertTrue("2.4", !task.isSubtypeOf(INVALID_MARKER));

			assertTrue("3.0", problem.isSubtypeOf(IMarker.MARKER));
			assertTrue("3.1", !problem.isSubtypeOf(IMarker.TASK));
			assertTrue("3.2", problem.isSubtypeOf(IMarker.PROBLEM));
			assertTrue("3.3", !problem.isSubtypeOf(TEST_PROBLEM_MARKER));
			assertTrue("3.4", !problem.isSubtypeOf(INVALID_MARKER));

			assertTrue("4.0", testProblem.isSubtypeOf(IMarker.MARKER));
			assertTrue("4.1", !testProblem.isSubtypeOf(IMarker.TASK));
			assertTrue("4.2", testProblem.isSubtypeOf(IMarker.PROBLEM));
			assertTrue("4.3", testProblem.isSubtypeOf(TEST_PROBLEM_MARKER));
			assertTrue("4.4", !testProblem.isSubtypeOf(INVALID_MARKER));

			// behaviour with an undefined marker type is not specified, but
			// test current behaviour to give us advance warning of accidental
			// behavioural change
			assertTrue("5.0", !invalid.isSubtypeOf(IMarker.MARKER));
			assertTrue("5.1", !invalid.isSubtypeOf(IMarker.TASK));
			assertTrue("5.2", !invalid.isSubtypeOf(IMarker.PROBLEM));
			assertTrue("5.3", !invalid.isSubtypeOf(TEST_PROBLEM_MARKER));
			assertTrue("5.4", invalid.isSubtypeOf(INVALID_MARKER));

		} catch (CoreException e) {
			fail("1.99", e);
		}

	}

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 */
	public void testMarkerChangesInDelta() {
		debug("TestMarkerChangesInDelta");

		// Create and register a listener.
		MarkersChangeListener listener = new MarkersChangeListener();
		addResourceChangeListener(listener);

		try {
			IResource resource;
			IMarker[] markers;
			IMarker marker;
			for (IResource resource2 : resources) {
				resource = resource2;
				markers = new IMarker[3];

				// ADD a marker
				listener.reset();
				try {
					markers[0] = resource.createMarker(IMarker.PROBLEM);
					assertExists("1.0." + resource.getFullPath(), markers[0]);
					assertEquals("1.1." + resource.getFullPath(), 1, listener.numAffectedResources());
					assertTrue("1.2." + resource.getFullPath(), listener.checkChanges(resource, new IMarker[] {markers[0]}, null, null));
				} catch (CoreException e) {
					fail("1.99." + resource.getFullPath(), e);
				}

				// ADD more markers to the same resource
				listener.reset();
				try {
					markers[1] = resource.createMarker(IMarker.BOOKMARK);
					markers[2] = resource.createMarker(IMarker.TASK);
					assertExists("2.0." + resource.getFullPath(), new IMarker[] {markers[1], markers[2]});
					assertEquals("2.1." + resource.getFullPath(), 1, listener.numAffectedResources());
					assertTrue("2.2." + resource.getFullPath(), listener.checkChanges(resource, new IMarker[] {markers[1], markers[2]}, null, null));
				} catch (CoreException e) {
					fail("2.99." + resource.getFullPath(), e);
				}

				// CHANGE a marker
				listener.reset();
				try {
					markers[0].setAttribute(IMarker.MESSAGE, "My text.");
					assertEquals("3.0." + resource.getFullPath(), 1, listener.numAffectedResources());
					assertTrue("3.1." + resource.getFullPath(), listener.checkChanges(resource, null, null, new IMarker[] {markers[0]}));
				} catch (CoreException e) {
					fail("3.99." + resource.getFullPath(), e);
				}

				// CHANGE more markers
				listener.reset();
				try {
					markers[1].setAttribute(IMarker.SEVERITY, "Low");
					markers[2].setAttribute(IMarker.PRIORITY, "Normal");
					assertEquals("4.0." + resource.getFullPath(), 1, listener.numAffectedResources());
					assertTrue("4.1." + resource.getFullPath(), listener.checkChanges(resource, null, null, new IMarker[] {markers[1], markers[2]}));
				} catch (CoreException e) {
					fail("4.99." + resource.getFullPath(), e);
				}

				// DELETE a marker
				listener.reset();
				try {
					markers[0].delete();
					assertDoesNotExist("5.0." + resource.getFullPath(), markers[0]);
					assertEquals("5.1." + resource.getFullPath(), 1, listener.numAffectedResources());
					assertTrue("5.2." + resource.getFullPath(), listener.checkChanges(resource, null, new IMarker[] {markers[0]}, null));
				} catch (CoreException e) {
					fail("5.99." + resource.getFullPath(), e);
				}

				// DELETE more markers
				listener.reset();
				try {
					resource.deleteMarkers(null, false, IResource.DEPTH_ZERO);
					assertDoesNotExist("6.0." + resource.getFullPath(), new IMarker[] {markers[1], markers[2]});
					assertEquals("6.1." + resource.getFullPath(), 1, listener.numAffectedResources());
					assertTrue("6.2." + resource.getFullPath(), listener.checkChanges(resource, null, new IMarker[] {markers[1], markers[2]}, null));
				} catch (CoreException e) {
					fail("6.99." + resource.getFullPath(), e);
				}

				// ADD, REMOVE and CHANGE markers
				try {
					markers[0] = resource.createMarker(IMarker.PROBLEM);
					markers[1] = resource.createMarker(IMarker.BOOKMARK);
					listener.reset();
					markers[0].delete();
					assertDoesNotExist("7.0." + resource.getFullPath(), markers[0]);
					markers[1].setAttribute(IMarker.MESSAGE, getRandomString());
					markers[2] = resource.createMarker(IMarker.TASK);
					assertEquals("7.1." + resource.getFullPath(), 1, listener.numAffectedResources());
					assertTrue("7.2." + resource.getFullPath(), listener.checkChanges(resource, new IMarker[] {markers[2]}, new IMarker[] {markers[0]}, new IMarker[] {markers[1]}));
				} catch (CoreException e) {
					fail("7.99." + resource.getFullPath(), e);
				}
			}

			// DELETE the resource and see what the marker delta is
			try {
				resource = getWorkspace().getRoot();
				resource.deleteMarkers(null, true, IResource.DEPTH_INFINITE);
				marker = resource.createMarker(IMarker.BOOKMARK);
				listener.reset();
				resource.delete(true, getMonitor());
				assertDoesNotExist("8.0", marker);
				assertTrue("8.1", listener.checkChanges(resource, null, new IMarker[] {marker}, null));
			} catch (CoreException e) {
				fail("8.99", e);
			}

		} finally {
			removeResourceChangeListener(listener);
		}
	}

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 * Particularly, checks that the MarkerDelta attributes reflect the
	 * state of the marker before the change occurred.
	 */
	public void testMarkerDeltaAttributes() {
		debug("testMarkerDeltaAttributes");

		// create markers on various resources
		final IMarker[] markers = new IMarker[3];
		IWorkspaceRunnable body = monitor -> {
			markers[0] = resources[0].createMarker(IMarker.BOOKMARK);
			markers[1] = resources[1].createMarker(IMarker.BOOKMARK);
			markers[1].setAttribute(IMarker.CHAR_START, 5);
			markers[2] = resources[2].createMarker(IMarker.PROBLEM);
			markers[2].setAttribute(IMarker.DONE, true);
			markers[2].setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
			markers[2].setAttribute(IMarker.MESSAGE, "Hello");
		};
		try {
			getWorkspace().run(body, getMonitor());
		} catch (CoreException e) {
			fail("0.99", e);
		}

		//create the attribute change listener
		MarkerAttributeChangeListener listener = new MarkerAttributeChangeListener();
		getWorkspace().addResourceChangeListener(listener);
		try {
			//add a new attribute
			listener.expectChanges(markers[0]);
			markers[0].setAttribute(IMarker.MESSAGE, "Message");
			listener.verifyChanges();

			//change an attribute
			listener.expectChanges(markers[0]);
			markers[0].setAttribute(IMarker.MESSAGE, "NewMessage");
			listener.verifyChanges();

			//remove an attribute
			listener.expectChanges(markers[0]);
			markers[0].setAttribute(IMarker.MESSAGE, null);
			listener.verifyChanges();

			//add attribute to marker that already has attributes
			listener.expectChanges(markers[2]);
			markers[2].setAttribute(IMarker.CHAR_END, 5);
			listener.verifyChanges();

			//add+change
			listener.expectChanges(markers[1]);
			getWorkspace().run((IWorkspaceRunnable) monitor -> {
				markers[1].setAttribute(IMarker.CHAR_START, 5);
				markers[1].setAttribute(IMarker.CHAR_END, 10);
			}, getMonitor());
			listener.verifyChanges();

			//change+remove same marker
			listener.expectChanges(markers[1]);
			getWorkspace().run((IWorkspaceRunnable) monitor -> {
				markers[1].setAttribute(IMarker.CHAR_START, 5);
				markers[1].setAttribute(IMarker.CHAR_START, null);
			}, getMonitor());
			listener.verifyChanges();

			//change multiple markers
			listener.expectChanges(markers);
			getWorkspace().run((IWorkspaceRunnable) monitor -> {
				markers[0].setAttribute(IMarker.CHAR_START, 5);
				markers[1].setAttribute(IMarker.CHAR_START, 10);
				markers[2].setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_LOW);
			}, getMonitor());
			listener.verifyChanges();
		} catch (CoreException e) {
			fail("1.99", e);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
	}

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 */
	public void testMarkerDeltasCopyResource() {
		debug("testMarkerDeltasCopyResource");

		// Create and register a listener.
		final MarkersChangeListener listener = new MarkersChangeListener();
		addResourceChangeListener(listener);

		try {
			// create markers on all the non-project resources
			final Hashtable<IResource, IMarker> table = new Hashtable<>(1);
			final int[] count = new int[1];
			count[0] = 0;
			IWorkspaceRunnable body = monitor -> {
				IResourceVisitor visitor = resource -> {
					if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT) {
						return true;
					}
					IMarker marker = resource.createMarker(IMarker.BOOKMARK);
					table.put(resource, marker);
					count[0]++;
					return true;
				};
				getWorkspace().getRoot().accept(visitor);
			};
			try {
				getWorkspace().run(body, getMonitor());
			} catch (CoreException e) {
				fail("0.99", e);
			}
			listener.reset();

			// copy all non-project resources
			try {
				IProject[] projects = getWorkspace().getRoot().getProjects();
				for (IProject project : projects) {
					IResource[] children = project.members();
					for (IResource element : children) {
						IPath destination = IPath.fromOSString(element.getName() + "copy");
						element.copy(destination, true, getMonitor());
					}
				}
			} catch (CoreException e) {
				fail("1.99", e);
			}

			// TODO
			// verify marker deltas
			//		IResourceVisitor visitor = new IResourceVisitor() {
			//			public boolean visit(IResource resource) throws CoreException {
			//				if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
			//					return true;
			//				if (!resource.getName().endsWith("copy"))
			//					return false;
			//				String name = resource.getFullPath().segment(0);
			//				IPath path = IPath.fromOSString(name.substring(0, name.length() - 4)).makeAbsolute();
			//				path = path.append(resource.getFullPath().removeFirstSegments(1));
			//				IResource oldResource = ((Workspace) getWorkspace()).newResource(path, resource.getType());
			//				IMarker marker = (IMarker) table.get(oldResource);
			//				assertNotNull("2.1." + oldResource.getFullPath(), marker);
			//				IMarker[] markers = resource.findMarkers(null, true, IResource.DEPTH_ZERO);
			//				assertEquals("2.2." + resource.getFullPath(), 1, markers.length);
			//				assertEquals("2.3." + resource.getFullPath(), marker.getId(), markers[0].getId());
			//				assertTrue("2.4." + resource.getFullPath(), listener.checkChanges(resource, new IMarker[] { markers[0] }, null, null));
			//				return true;
			//			}
			//		};
			// marker deltas should not appear after a resource copy
			//assertEquals("2.5", count[0], listener.numAffectedResources());
			assertEquals("2.5", 0, listener.numAffectedResources());
			//try {
			//getWorkspace().getRoot().accept(visitor);
			//} catch (CoreException e) {
			//fail("2.99", e);
			//}
		} finally {
			removeResourceChangeListener(listener);
		}
	}

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 */
	public void testMarkerDeltasMerge() {
		debug("testMarkerDeltasMerge");

		// Create and register a listener.
		final MarkersChangeListener listener = new MarkersChangeListener();
		addResourceChangeListener(listener);

		try {
			for (final IResource resource : resources) {
				// ADD + REMOVE = nothing
				try {
					IWorkspaceRunnable body = monitor -> {
						listener.reset();
						IMarker marker = resource.createMarker(IMarker.PROBLEM);
						assertExists("1.0." + resource.getFullPath(), marker);
						marker.delete();
						assertDoesNotExist("1.1." + resource.getFullPath(), marker);
					};
					getWorkspace().run(body, getMonitor());
					assertEquals("1.2." + resource.getFullPath(), 0, listener.numAffectedResources());
					assertTrue("1.3." + resource.getFullPath(), listener.checkChanges(resource, null, null, null));
				} catch (CoreException e) {
					fail("1.99." + resource.getFullPath(), e);
				}

				// ADD + CHANGE = ADD
				try {
					// cannot re-assign variable value within the code below, so must
					// put our marker value inside an array and set the element.
					final IMarker[] markers = new IMarker[1];
					IWorkspaceRunnable body = monitor -> {
						listener.reset();
						markers[0] = resource.createMarker(IMarker.PROBLEM);
						assertExists("2.0." + resource.getFullPath(), markers[0]);
						markers[0].setAttribute(IMarker.MESSAGE, "my message text");
						assertEquals("2.1." + resource.getFullPath(), "my message text", markers[0].getAttribute(IMarker.MESSAGE));
					};
					getWorkspace().run(body, getMonitor());
					assertEquals("2.2." + resource.getFullPath(), 1, listener.numAffectedResources());
					assertTrue("2.3." + resource.getFullPath(), listener.checkChanges(resource, new IMarker[] {markers[0]}, null, null));
				} catch (CoreException e) {
					fail("2.99." + resource.getFullPath(), e);
				}

				// TODO
				// REMOVE + ADD = CHANGE
				//try {
				//final IMarker[] markers = new IMarker[1];
				//markers[0] = resource.createMarker(IMarker.PROBLEM);
				//assertExists("3.0." + resource.getFullPath(), markers[0]);
				//IWorkspaceRunnable body = new IWorkspaceRunnable() {
				//public void run(IProgressMonitor monitor) throws CoreException {
				//listener.reset();
				//markers[0].delete();
				//assertDoesNotExist("3.1." + resource.getFullPath(), markers[0]);
				//markers[0] = resource.createMarker(IMarker.PROBLEM);
				//assertExists("3.2." + resource.getFullPath(), markers[0]);
				//}
				//};
				//getWorkspace().run(body, getMonitor());
				//assertEquals("3.3." + resource.getFullPath(), 1, listener.numAffectedResources());
				//assert("3.4." + resource.getFullPath(), listener.checkChanges(resource, null, null, new IMarker[] { markers[0] }));
				//} catch (CoreException e) {
				//fail("3.99." + resource.getFullPath(), e);
				//}

				// CHANGE + CHANGE = CHANGE
				try {
					final IMarker[] markers = new IMarker[1];
					markers[0] = resource.createMarker(IMarker.PROBLEM);
					assertExists("4.0." + resource.getFullPath(), markers[0]);
					IWorkspaceRunnable body = monitor -> {
						listener.reset();
						markers[0].setAttribute(IMarker.MESSAGE, "my message text");
						assertEquals("4.1." + resource.getFullPath(), "my message text", markers[0].getAttribute(IMarker.MESSAGE));
						markers[0].setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
						assertEquals("4.2." + resource.getFullPath(), IMarker.PRIORITY_HIGH, ((Integer) markers[0].getAttribute(IMarker.PRIORITY)).intValue());
					};
					getWorkspace().run(body, getMonitor());
					assertEquals("4.3." + resource.getFullPath(), 1, listener.numAffectedResources());
					assertTrue("4.4." + resource.getFullPath(), listener.checkChanges(resource, null, null, new IMarker[] {markers[0]}));
				} catch (CoreException e) {
					fail("4.99." + resource.getFullPath(), e);
				}

				// CHANGE + REMOVE = REMOVE
				try {
					final IMarker[] markers = new IMarker[1];
					markers[0] = resource.createMarker(IMarker.PROBLEM);
					assertExists("5.0." + resource.getFullPath(), markers[0]);
					IWorkspaceRunnable body = monitor -> {
						listener.reset();
						markers[0].setAttribute(IMarker.MESSAGE, "my message text");
						assertEquals("5.1." + resource.getFullPath(), "my message text", markers[0].getAttribute(IMarker.MESSAGE));
						markers[0].delete();
						assertDoesNotExist("5.2." + resource.getFullPath(), markers[0]);
					};
					getWorkspace().run(body, getMonitor());
					assertEquals("5.3." + resource.getFullPath(), 1, listener.numAffectedResources());
					assertTrue("5.4." + resource.getFullPath(), listener.checkChanges(resource, null, new IMarker[] {markers[0]}, null));
				} catch (CoreException e) {
					fail("5.99." + resource.getFullPath(), e);
				}

				// cleanup after each iteration
				try {
					resource.deleteMarkers(null, true, IResource.DEPTH_ZERO);
				} catch (CoreException e) {
					fail("99.99", e);
				}
			}
		} finally {
			removeResourceChangeListener(listener);
		}
	}

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 */
	public void testMarkerDeltasMoveFolder() {
		debug("testMarkerDeltasMoveFolder");

		IWorkspaceRoot root = getWorkspace().getRoot();
		final IProject project = root.getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subFile.txt");
		ensureExistsInWorkspace(new IResource[] {project, folder, file, subFile}, true);
		waitForEncodingRelatedJobs();
		IFolder destFolder = project.getFolder("myOtherFolder");
		IFile destSubFile = destFolder.getFile(subFile.getName());
		IMarker folderMarker = null;
		IMarker subFileMarker = null;
		IMarker[] markers = null;

		// Create and register a listener.
		MarkersChangeListener listener = new MarkersChangeListener();
		addResourceChangeListener(listener);
		try {
			// create markers on the resources
			try {
				folderMarker = folder.createMarker(IMarker.BOOKMARK);
			} catch (CoreException e) {
				fail("1.0", e);
			}
			try {
				subFileMarker = subFile.createMarker(IMarker.BOOKMARK);
			} catch (CoreException e) {
				fail("1.1", e);
			}
		} finally {
			removeResourceChangeListener(listener);
		}

		listener = new MarkersChangeListener();
		addResourceChangeListener(listener);
		try {
			// move the files
			try {
				folder.move(destFolder.getFullPath(), IResource.FORCE, getMonitor());
			} catch (CoreException e) {
				fail("2.0", e);
			}

			// verify marker deltas
			assertTrue("3.1", listener.checkChanges(folder, null, new IMarker[] {folderMarker}, null));
			try {
				markers = destFolder.findMarkers(null, true, IResource.DEPTH_ZERO);
			} catch (CoreException e) {
				fail("3.2", e);
			}
			assertEquals("3.3", 1, markers.length);
			assertEquals("3.4", folderMarker.getId(), markers[0].getId());
			assertTrue("3.5", listener.checkChanges(destFolder, new IMarker[] {markers[0]}, null, null));

			assertTrue("3.7", listener.checkChanges(subFile, null, new IMarker[] {subFileMarker}, null));
			try {
				markers = destSubFile.findMarkers(null, true, IResource.DEPTH_ZERO);
			} catch (CoreException e) {
				fail("3.8", e);
			}
			assertEquals("3.9", 1, markers.length);
			assertEquals("3.10", subFileMarker.getId(), markers[0].getId());
			assertTrue("3.11", listener.checkChanges(destSubFile, new IMarker[] {markers[0]}, null, null));

		} finally {
			removeResourceChangeListener(listener);
		}
	}

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 */
	public void testMarkerDeltasMoveFile() {
		debug("testMarkerDeltasMoveFile");
		IWorkspaceRoot root = getWorkspace().getRoot();
		final IProject project = root.getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subFile.txt");
		ensureExistsInWorkspace(new IResource[] {project, folder, file, subFile}, true);
		waitForEncodingRelatedJobs();
		IFile destFile = folder.getFile(file.getName());
		IFile destSubFile = project.getFile(subFile.getName());
		IMarker fileMarker = null;
		IMarker subFileMarker = null;
		IMarker[] markers = null;

		// Create and register a listener.
		final MarkersChangeListener listener = new MarkersChangeListener();
		addResourceChangeListener(listener);

		try {
			// create markers on the resources
			try {
				fileMarker = file.createMarker(IMarker.BOOKMARK);
			} catch (CoreException e) {
				fail("1.0", e);
			}
			try {
				subFileMarker = subFile.createMarker(IMarker.BOOKMARK);
			} catch (CoreException e) {
				fail("1.1", e);
			}
			listener.reset();

			// move the files
			try {
				file.move(destFile.getFullPath(), IResource.FORCE, getMonitor());
				subFile.move(destSubFile.getFullPath(), IResource.FORCE, getMonitor());
			} catch (CoreException e) {
				fail("2.0", e);
			}

			// verify marker deltas
			assertTrue("3.1", listener.checkChanges(file, null, new IMarker[] {fileMarker}, null));
			try {
				markers = destFile.findMarkers(null, true, IResource.DEPTH_ZERO);
			} catch (CoreException e) {
				fail("3.2", e);
			}
			assertEquals("3.3", 1, markers.length);
			assertEquals("3.4", fileMarker.getId(), markers[0].getId());
			assertTrue("3.5", listener.checkChanges(destFile, new IMarker[] {markers[0]}, null, null));

			assertTrue("3.7", listener.checkChanges(subFile, null, new IMarker[] {subFileMarker}, null));
			try {
				markers = destSubFile.findMarkers(null, true, IResource.DEPTH_ZERO);
			} catch (CoreException e) {
				fail("3.8", e);
			}
			assertEquals("3.9", 1, markers.length);
			assertEquals("3.10", subFileMarker.getId(), markers[0].getId());
			assertTrue("3.11", listener.checkChanges(destSubFile, new IMarker[] {markers[0]}, null, null));

		} finally {
			removeResourceChangeListener(listener);
		}
	}

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 */
	public void testMarkerDeltasMoveProject() {
		debug("testMarkerDeltasMoveProject");

		// Create and register a listener.
		final MarkersChangeListener listener = new MarkersChangeListener();
		addResourceChangeListener(listener);

		try {
			// create markers on all the resources
			final Hashtable<IResource, IMarker> table = new Hashtable<>(1);
			final int[] count = new int[1];
			count[0] = 0;
			IWorkspaceRunnable body = monitor -> {
				IResourceVisitor visitor = resource -> {
					if (resource.getType() == IResource.ROOT) {
						return true;
					}
					IMarker marker = resource.createMarker(IMarker.BOOKMARK);
					table.put(resource, marker);
					count[0]++;
					return true;
				};
				getWorkspace().getRoot().accept(visitor);
			};
			try {
				getWorkspace().run(body, getMonitor());
			} catch (CoreException e) {
				fail("0.99", e);
			}
			listener.reset();

			// move all resources
			IProject[] projects = getWorkspace().getRoot().getProjects();
			for (IProject project : projects) {
				IPath destination = IPath.fromOSString(project.getName() + "move");
				try {
					project.move(destination, true, getMonitor());
				} catch (CoreException e) {
					fail("1.99", e);
				}
			}

			// verify marker deltas
			IResourceVisitor visitor = resource -> {
				if (resource.getType() == IResource.ROOT) {
					return true;
				}
				String name = resource.getFullPath().segment(0);
				IPath path = IPath.fromOSString(name.substring(0, name.length() - 4)).makeAbsolute();
				path = path.append(resource.getFullPath().removeFirstSegments(1));
				IResource oldResource = ((Workspace) getWorkspace()).newResource(path, resource.getType());
				IMarker marker = table.get(oldResource);
				assertNotNull("2.1." + oldResource.getFullPath(), marker);
				assertTrue("2.2." + oldResource.getFullPath(), listener.checkChanges(oldResource, null, new IMarker[] {marker}, null));
				IMarker[] markers = resource.findMarkers(null, true, IResource.DEPTH_ZERO);
				assertEquals("2.3." + resource.getFullPath(), 1, markers.length);
				assertEquals("2.4." + resource.getFullPath(), marker.getId(), markers[0].getId());
				assertTrue("2.5." + resource.getFullPath(), listener.checkChanges(resource, new IMarker[] {markers[0]}, null, null));
				return true;
			};
			assertEquals("2.6", count[0] * 2, listener.numAffectedResources());
			try {
				getWorkspace().getRoot().accept(visitor);
			} catch (CoreException e) {
				fail("2.99", e);
			}
		} finally {
			removeResourceChangeListener(listener);
		}
	}

	public void testMarkerSave() {
		debug("TestMarkerSave");

		IMarker[] newMarkers = null;
		IMarker[] expected = null;
		try {
			newMarkers = createMarkers(resources, IMarker.PROBLEM);
			expected = new IMarker[newMarkers.length * 3];
			System.arraycopy(newMarkers, 0, expected, 0, newMarkers.length);
			newMarkers = createMarkers(resources, IMarker.BOOKMARK);
			System.arraycopy(newMarkers, 0, expected, newMarkers.length, newMarkers.length);
			newMarkers = createMarkers(resources, IMarker.TASK);
			System.arraycopy(newMarkers, 0, expected, newMarkers.length * 2, newMarkers.length);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		final MarkerManager manager = ((Workspace) getWorkspace()).getMarkerManager();

		// write all the markers to the output stream
		File file = Platform.getLocation().append(".testmarkers").toFile();
		OutputStream fileOutput = null;
		DataOutputStream o1 = null;
		try {
			fileOutput = new FileOutputStream(file);
			o1 = new DataOutputStream(fileOutput);
		} catch (FileNotFoundException e) {
			fail("2.0", e);
		}
		final DataOutputStream output = o1;
		final List<String> list = new ArrayList<>(5);
		IResourceVisitor visitor = resource -> {
			try {
				ResourceInfo info = ((Resource) resource).getResourceInfo(false, false);
				if (info == null) {
					return true;
				}
				IPathRequestor requestor = new IPathRequestor() {
					@Override
					public IPath requestPath() {
						return resource.getFullPath();
					}

					@Override
					public String requestName() {
						return resource.getName();
					}
				};
				manager.save(info, requestor, output, list);
			} catch (IOException e) {
				fail("2.1", e);
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("2.2", e);
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				fail("2.3", e);
			}
		}

		// delete all markers resources
		try {
			getWorkspace().getRoot().deleteMarkers(null, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		assertDoesNotExist("3.1", expected);

		// read in the markers from the file
		try {
			InputStream fileInput = new FileInputStream(file);
			final DataInputStream input = new DataInputStream(fileInput);
			IWorkspaceRunnable body = monitor -> {
				MarkerReader reader = new MarkerReader((Workspace) getWorkspace());
				try {
					reader.read(input, true);
				} catch (IOException e) {
					fail("4.0", e);
				}
			};
			try {
				getWorkspace().run(body, getMonitor());
			} finally {
				try {
					input.close();
				} catch (IOException e) {
					fail("4.1", e);
				}
			}
		} catch (FileNotFoundException e) {
			fail("4.2", e);
		} catch (CoreException e) {
			fail("4.3", e);
		}

		// assert that the markers retrieved are the same as the ones we used
		// to have
		try {
			assertExists("5.0", expected);
			IMarker[] actual = getWorkspace().getRoot().findMarkers(null, false, IResource.DEPTH_INFINITE);
			assertEquals("5.1", expected, actual);
		} catch (CoreException e) {
			fail("5.2", e);
		}

		// cleanup
		assertTrue("6.0", file.delete());
	}

	public void testMarkerSaveTransient() {
		debug("TestMarkerSaveTransient");

		// create the markers on the resources. create both transient
		// and persistent markers.
		final ArrayList<IMarker> persistentMarkers = new ArrayList<>();
		IResourceVisitor visitor = resource -> {
			IMarker marker = resource.createMarker(IMarker.PROBLEM);
			persistentMarkers.add(marker);
			marker = resource.createMarker(IMarker.BOOKMARK);
			persistentMarkers.add(marker);
			marker = resource.createMarker(TRANSIENT_MARKER);
			// create a transient marker of a persistent type
			marker = resource.createMarker(IMarker.BOOKMARK);
			marker.setAttribute(IMarker.TRANSIENT, Boolean.TRUE);
			// create a marker of a persistent type and set TRANSIENT as false (should be persisted)
			marker = resource.createMarker(IMarker.BOOKMARK);
			marker.setAttribute(IMarker.TRANSIENT, Boolean.FALSE);
			persistentMarkers.add(marker);
			// create a marker of a transient type and set TRANSIENT to false (should NOT be persisted)
			marker = resource.createMarker(TRANSIENT_MARKER);
			marker.setAttribute(IMarker.TRANSIENT, Boolean.FALSE);
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		final MarkerManager manager = ((Workspace) getWorkspace()).getMarkerManager();
		IMarker[] expected = persistentMarkers.toArray(new IMarker[persistentMarkers.size()]);

		// write all the markers to the output stream
		File file = Platform.getLocation().append(".testmarkers").toFile();
		OutputStream fileOutput = null;
		DataOutputStream o1 = null;
		try {
			fileOutput = new FileOutputStream(file);
			o1 = new DataOutputStream(fileOutput);
		} catch (FileNotFoundException e) {
			fail("2.0", e);
		}
		final DataOutputStream output = o1;
		final List<String> list = new ArrayList<>(5);
		visitor = resource -> {
			try {
				ResourceInfo info = ((Resource) resource).getResourceInfo(false, false);
				if (info == null) {
					return true;
				}
				IPathRequestor requestor = new IPathRequestor() {
					@Override
					public IPath requestPath() {
						return resource.getFullPath();
					}

					@Override
					public String requestName() {
						return resource.getName();
					}
				};
				manager.save(info, requestor, output, list);
			} catch (IOException e) {
				fail("2.1", e);
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("2.2", e);
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				fail("2.3", e);
			}
		}

		// delete all markers resources
		try {
			getWorkspace().getRoot().deleteMarkers(null, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		assertDoesNotExist("3.1", expected);

		// read in the markers from the file
		try {
			InputStream fileInput = new FileInputStream(file);
			final DataInputStream input = new DataInputStream(fileInput);
			IWorkspaceRunnable body = monitor -> {
				MarkerReader reader = new MarkerReader((Workspace) getWorkspace());
				try {
					reader.read(input, true);
				} catch (IOException e) {
					fail("4.0", e);
				}
			};
			try {
				getWorkspace().run(body, getMonitor());
			} finally {
				try {
					input.close();
				} catch (IOException e) {
					fail("4.1", e);
				}
			}
		} catch (FileNotFoundException e) {
			fail("4.2", e);
		} catch (CoreException e) {
			fail("4.3", e);
		}

		// assert that the markers retrieved are the same as the ones we used
		// to have
		try {
			assertExists("5.0", expected);
			IMarker[] actual = getWorkspace().getRoot().findMarkers(null, false, IResource.DEPTH_INFINITE);
			assertEquals("5.1", expected, actual);
		} catch (CoreException e) {
			fail("5.2", e);
		}

		// cleanup
		assertTrue("6.0", file.delete());
	}

	/**
	 * Tests whether markers correctly move with resources.
	 */
	public void testMoveResource() {
		debug("TestMoveResource");
	}

	/*
	 * Test for PR: "1FWT3V5: ITPCORE:WINNT - Task view shows entries for closed projects"
	 */
	public void testProjectCloseOpen() {
		debug("testProjectCloseOpen");
		IProject project = null;
		IMarker marker = null;

		// create a marker on the project
		project = getWorkspace().getRoot().getProjects()[0];
		try {
			marker = project.createMarker(IMarker.BOOKMARK);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// ensure that the marker was created
		assertTrue("2.0", marker.exists());

		// close the project
		try {
			project.close(getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		// now the marker should be inaccessible
		assertTrue("", !marker.exists());

		// open the project
		try {
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}

		// the marker re-appears
		assertTrue("5.0", marker.exists());
	}

	public void testSetGetAttribute() {
		debug("testSetGetAttribute");

		for (IResource resource : resources) {
			IMarker marker = null;

			// getting a non-existant attribute should return null
			try {
				marker = resource.createMarker(IMarker.PROBLEM);
			} catch (CoreException e) {
				fail("1.0", e);
			}
			try {
				assertNull("1.1." + resource.getFullPath(), marker.getAttribute(IMarker.MESSAGE));
			} catch (CoreException e) {
				fail("1.2." + resource.getFullPath(), e);
			}

			// set an attribute, get its value, then remove it
			String testMessage = getRandomString();
			try {
				marker.setAttribute(IMarker.MESSAGE, testMessage);
				Object value = marker.getAttribute(IMarker.MESSAGE);
				assertEquals("2.0." + resource.getFullPath(), testMessage, (String) value);
				marker.setAttribute(IMarker.MESSAGE, null);
				value = marker.getAttribute(IMarker.MESSAGE);
				assertNull("2.1." + resource.getFullPath(), value);
			} catch (CoreException e) {
				fail("2.2." + resource.getFullPath(), e);
			}

			// set more attributes, get their values, then remove one
			try {
				String[] keys = new String[] {IMarker.LOCATION, IMarker.SEVERITY, IMarker.DONE};
				Object[] values = new Object[3];
				values[0] = getRandomString();
				values[1] = Integer.valueOf(5);
				values[2] = Boolean.FALSE;
				marker.setAttributes(keys, values);
				Object[] found = marker.getAttributes(keys);
				assertEquals("3.0." + resource.getFullPath(), values, found);
				marker.setAttribute(IMarker.SEVERITY, null);
				values[1] = null;
				found = marker.getAttributes(keys);
				assertEquals("3.1." + resource.getFullPath(), values, found);
				values[1] = Integer.valueOf(5);
				marker.setAttribute(IMarker.SEVERITY, values[1]);
				Map<String, ? extends Object> all = marker.getAttributes();
				assertEquals("3.2." + resource.getFullPath(), all, keys, values);
			} catch (CoreException e) {
				fail("3.2." + resource.getFullPath(), e);
			}

			// try sending null as args
			try {
				marker.getAttribute(null);
				fail("4.0");
			} catch (CoreException e) {
				fail("4.1." + resource.getFullPath(), e);
			} catch (NullPointerException e) {
				fail("4.2", e);
			} catch (RuntimeException e) {
				// expected
			}
			try {
				marker.getAttributes(null);
				fail("4.3");
			} catch (CoreException e) {
				fail("4.4." + resource.getFullPath(), e);
			} catch (NullPointerException e) {
				fail("4.5", e);
			} catch (RuntimeException e) {
				// expected
			}
			try {
				marker.setAttribute(null, getRandomString());
				fail("4.6");
			} catch (CoreException e) {
				fail("4.7." + resource.getFullPath(), e);
			} catch (NullPointerException e) {
				fail("4.8", e);
			} catch (RuntimeException e) {
				// expected
			}
			try {
				marker.setAttributes(null, new String[] {getRandomString()});
				fail("4.9");
			} catch (CoreException e) {
				fail("4.10." + resource.getFullPath(), e);
			} catch (NullPointerException e) {
				fail("4.11", e);
			} catch (RuntimeException e) {
				// expected
			}
			try {
				marker.setAttributes(new String[] {IMarker.MESSAGE}, null);
				fail("4.12");
			} catch (CoreException e) {
				fail("4.13." + resource.getFullPath(), e);
			} catch (NullPointerException e) {
				fail("4.14", e);
			} catch (RuntimeException e) {
				// expected
			}
			//set attributes on deleted marker
			try {
				marker.delete();
			} catch (CoreException e) {
				fail("5.0", e);
			}
			try {
				marker.setAttribute(IMarker.MESSAGE, "Hello");
				fail("5.1");
			} catch (CoreException e) {
				// expected
			}
			try {
				marker.setAttributes(new String[] {IMarker.LINE_NUMBER}, new Object[] {Integer.valueOf(4)});
				fail("5.2");
			} catch (CoreException e) {
				// expected
			}
			try {
				HashMap<String, String> attributes = new HashMap<>();
				attributes.put(IMarker.MESSAGE, "Hello");
				marker.setAttributes(attributes);
				fail("5.3");
			} catch (CoreException e) {
				// expected
			}
		}
	}

	public void testGetAttributesEquality() throws Exception {
		final String value = "Some value";
		for (int i = 0; i < resources.length; i++) {
			IMarker marker = resources[i].createMarker(IMarker.PROBLEM, Map.of(createNewString(i), value));

			// Check we can get the value by equal key
			assertEquals(value, marker.getAttribute(createNewString(i)));

			// Check the map returned by marker is equal to equal map
			Map<String, Object> existing = marker.getAttributes();
			Map<String, Object> otherAttributes = Map.of(createNewString(i), value);
			assertEquals(existing, otherAttributes);
		}
	}

	private String createNewString(int i) {
		return new StringBuilder().append(i).toString();
	}

	public void testSetGetAttribute2() {
		debug("testSetGetAttribute2");

		for (IResource resource : resources) {
			IMarker marker = null;

			// getting a non-existant attribute should return null or the specified default
			try {
				marker = resource.createMarker(IMarker.PROBLEM);
			} catch (CoreException e) {
				fail("0.0" + resource.getFullPath(), e);
			}
			try {
				// #getAttribute(Object)
				assertNull("1.0." + resource.getFullPath(), marker.getAttribute(IMarker.MESSAGE));
				// #getAttribute(String, String)
				assertEquals("1.1." + resource.getFullPath(), "default", marker.getAttribute(IMarker.MESSAGE, "default"));
				// #getAttribute(String, boolean)
				assertEquals("1.2." + resource.getFullPath(), true, marker.getAttribute(IMarker.MESSAGE, true));
				// #getAttribute(String, int)
				assertEquals("1.3." + resource.getFullPath(), 5, marker.getAttribute(IMarker.MESSAGE, 5));
				// #getAttributes()
				assertNull("1.4." + resource.getFullPath(), marker.getAttributes());
				// #getAttributes(String[])
				assertTrue("1.5." + resource.getFullPath(), marker.getAttributes(new String[] {IMarker.MESSAGE})[0] == null);
			} catch (CoreException e) {
				fail("1.99." + resource.getFullPath(), e);
			}

			// set an attribute, get its value, then remove it
			String testMessage = getRandomString();
			try {
				marker.setAttribute(IMarker.MESSAGE, testMessage);
				Object value = marker.getAttribute(IMarker.MESSAGE);
				assertEquals("2.0." + resource.getFullPath(), testMessage, (String) value);
				marker.setAttribute(IMarker.MESSAGE, null);
				value = marker.getAttribute(IMarker.MESSAGE);
				assertNull("2.1." + resource.getFullPath(), value);
			} catch (CoreException e) {
				fail("2.2." + resource.getFullPath(), e);
			}

			// set more attributes, get their values, then remove one
			try {
				String[] keys = new String[] {IMarker.LOCATION, IMarker.SEVERITY, IMarker.DONE};
				Object[] values = new Object[3];
				values[0] = getRandomString();
				values[1] = Integer.valueOf(5);
				values[2] = Boolean.FALSE;
				marker.setAttributes(keys, values);
				Object[] found = marker.getAttributes(keys);
				assertEquals("3.0." + resource.getFullPath(), values, found);
				marker.setAttribute(IMarker.SEVERITY, null);
				values[1] = null;
				found = marker.getAttributes(keys);
				assertEquals("3.1." + resource.getFullPath(), values, found);
				values[1] = Integer.valueOf(5);
				marker.setAttribute(IMarker.SEVERITY, values[1]);
				Map<String, ? extends Object> all = marker.getAttributes();
				assertEquals("3.2." + resource.getFullPath(), all, keys, values);
			} catch (CoreException e) {
				fail("3.2." + resource.getFullPath(), e);
			}

			// try sending null as args
			try {
				// #getAttribute(String)
				marker.getAttribute(null);
				fail("4.0");
			} catch (CoreException e) {
				fail("4.1." + resource.getFullPath(), e);
			} catch (NullPointerException e) {
				fail("4.2", e);
			} catch (RuntimeException e) {
				// expected
			}
			try {
				// #getAttribute(String, String)
				marker.getAttribute(null, "default");
				fail("4.3");
			} catch (NullPointerException e) {
				fail("4.5", e);
			} catch (RuntimeException e) {
				// expected
			}
			try {
				// #getAttribute(String, boolean)
				marker.getAttribute(null, true);
				fail("4.6");
			} catch (NullPointerException e) {
				fail("4.8", e);
			} catch (RuntimeException e) {
				// expected
			}
			try {
				// #getAttribute(String, int)
				marker.getAttribute(null, 5);
				fail("4.9");
			} catch (NullPointerException e) {
				fail("4.11", e);
			} catch (RuntimeException e) {
				// expected
			}
			try {
				// #getAttributes(String[])
				marker.getAttributes(null);
				fail("4.12");
			} catch (CoreException e) {
				fail("4.13." + resource.getFullPath(), e);
			} catch (NullPointerException e) {
				fail("4.14", e);
			} catch (RuntimeException e) {
				// expected
			}
			try {
				// #setAttribute(String, Object)
				marker.setAttribute(null, getRandomString());
				fail("4.15");
			} catch (CoreException e) {
				fail("4.16." + resource.getFullPath(), e);
			} catch (NullPointerException e) {
				fail("4.17", e);
			} catch (RuntimeException e) {
				// expected
			}
			try {
				// #setAttributes(String[], Object[])
				marker.setAttributes(null, new String[] {getRandomString()});
				fail("4.18");
			} catch (CoreException e) {
				fail("4.19." + resource.getFullPath(), e);
			} catch (NullPointerException e) {
				fail("4.20", e);
			} catch (RuntimeException e) {
				// expected
			}
			try {
				// #setAttributes(String[], Object[])
				marker.setAttributes(new String[] {IMarker.MESSAGE}, null);
				fail("4.21");
			} catch (CoreException e) {
				fail("4.22." + resource.getFullPath(), e);
			} catch (NullPointerException e) {
				fail("4.23", e);
			} catch (RuntimeException e) {
				// expected
			}
			try {
				Map<String, Object> map2 = marker.getAttributes();
				map2.put("1", null); // allowed for clients using IMarker.getAttributes()
				map2.put("2", 2);
				marker.setAttributes(map2);
				assertNull(marker.getAttribute("1"));
				assertEquals(2, marker.getAttribute("2"));
				map2.put(null, 1); // allowed for clients using IMarker.getAttributes()
			} catch (CoreException e) {
				fail("4.24." + resource.getFullPath(), e);
			}
			try {
				Map<String, Object> map2 = marker.getAttributes();
				map2.put(null, 1); // allowed for clients using IMarker.getAttributes()
				try {
					marker.setAttributes(map2); // not allowed for clients to put null key
					fail("4.25");
				} catch (Exception e) {
					// expected
				}
			} catch (CoreException e) {
				fail("4.26" + resource.getFullPath(), e);
			}
			try {
				assertNotNull(marker.getAttribute("2"));
				assertNotNull(marker.getAttributes());
				marker.setAttributes(null);
				assertNull(marker.getAttribute("1"));
				assertNull(marker.getAttribute("2"));
				assertNull(marker.getAttributes());
			} catch (CoreException e) {
				fail("4.27" + resource.getFullPath(), e);
			}
		}
	}
}
