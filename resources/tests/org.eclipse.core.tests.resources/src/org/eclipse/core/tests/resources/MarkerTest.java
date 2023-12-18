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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForEncodingRelatedJobs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.concurrent.atomic.AtomicReference;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class MarkerTest {

	@Rule
	public TestName testName = new TestName();

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	public static final String TRANSIENT_MARKER = "org.eclipse.core.tests.resources.transientmarker";
	public static final String TEST_PROBLEM_MARKER = "org.eclipse.core.tests.resources.testproblem";

	/** The collection of resources used for testing. */
	IResource[] resources;
	private boolean originalRefreshSetting;
	private MarkersChangeListener registeredResourceChangeLister;

	private void setResourceChangeListener(MarkersChangeListener listener) throws CoreException {
		// removeResourceChangeListener need to happen in an atomic workspace operation
		// otherwise it would be removed while auto refresh is running
		// and might even get called in another thread after removing in this thread
		// listener.shutDown();
		if (registeredResourceChangeLister != null) {
			getWorkspace().run(p -> getWorkspace().removeResourceChangeListener(registeredResourceChangeLister), null);
		}
		registeredResourceChangeLister = listener;
		if (listener == null) {
			return;
		}
		// addResourceChangeListener need to happen in an atomic workspace operation
		// otherwise it would be added while auto refresh is running
		// and might get called in another thread before explicit refresh in this thread
		getWorkspace().run(p -> {
			getWorkspace().addResourceChangeListener(listener);
			// listener.active();
		}, null);
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

	protected void assertMarkersDoNotExist(IMarker[] markers) {
		for (IMarker marker : markers) {
			assertMarkerDoesNotExist(marker);
		}
	}

	protected void assertMarkerDoesNotExist(IMarker marker) {
		assertFalse(String.format("marker '%s' exists unexpectedly for resource '%s'", marker, marker.getResource()),
				marker.exists());
	}

	protected void assertMarkersExist(IMarker[] markers) {
		for (IMarker marker : markers) {
			assertMarkerExists(marker);
		}
	}

	protected void assertMarkerExists(IMarker marker) {
		assertTrue(String.format("marker '%s' does not exist for resource '%s'", marker, marker.getResource()),
				marker.exists());
	}

	private void assertMarkerHasAttributeValue(IMarker marker, String attributeName, Object expectedValue)
			throws CoreException {
		assertThat("marker has unexpected attribute value: " + marker, marker.getAttribute(attributeName),
				expectedValue == null ? is(nullValue()) : is(expectedValue));
	}

	private void assertSingleMarkerWithId(IMarker[] markers, long id) {
		assertThat(markers, arrayWithSize(1));
		assertThat("wrong id in marker " + markers[0], markers[0].getId(), is(id));
	}

	private void assertMarkerIsSubtype(IMarker marker, String superType) throws CoreException {
		assertTrue(String.format("Marker '%s' is no subtype of %s", marker, superType), marker.isSubtypeOf(superType));
	}

	private void assertMarkerIsNoSubtype(IMarker marker, String superType) throws CoreException {
		assertFalse(String.format("Marker '%s' is subtype of %s", marker, superType), marker.isSubtypeOf(superType));
	}

	public IResource[] createLargeHierarchy() throws CoreException {
		ArrayList<String> result = new ArrayList<>();
		result.add("/");
		new MarkerTest().addChildren(result, IPath.ROOT, 3, 4);
		String[] names = result.toArray(new String[result.size()]);
		IResource[] created = buildResources(getWorkspace().getRoot(), names);
		createInWorkspace(created);
		return created;
	}

	protected IMarker[] createMarkers(final IResource[] hosts, final String type) throws CoreException {
		final IMarker[] result = new IMarker[hosts.length];
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			for (int i = 0; i < hosts.length; i++) {
				result[i] = hosts[i].createMarker(type);
			}
		}, createTestMonitor());
		return result;
	}

	public void createProblem(IResource host, int severity) throws CoreException {
		IMarker marker = host.createMarker(IMarker.PROBLEM);
		marker.setAttribute(IMarker.SEVERITY, severity);
	}

	@Before
	public void setUp() throws Exception {
		resources = buildResources(getWorkspace().getRoot(),
				new String[] { "/", "1/", "1/1", "1/2/", "1/2/1", "1/2/2/", "2/", "2/1", "2/2/", "2/2/1", "2/2/2/" });
		createInWorkspace(resources);

		// disable autorefresh an wait till that is finished
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		originalRefreshSetting = prefs.getBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, false);
		prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, false);
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, null);
	}


	@After
	public void tearDown() throws Exception {
		if (registeredResourceChangeLister != null) {
			setResourceChangeListener(null);
		}
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, originalRefreshSetting);
	}

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 */
	@Test
	public void testMarkerChangesInDelta3() throws CoreException {
		// Create and register a listener.
		final MarkersChangeListener listener = new MarkersChangeListener();
		setResourceChangeListener(listener);

		IResource resource = getWorkspace().getRoot().findMember("1");
		IMarker[] markers = new IMarker[4];

		// MOVE the resource
		IPath destination = resource.getFullPath().removeLastSegments(1)
				.append(resource.getFullPath().lastSegment() + "new");
		markers[0] = resource.createMarker(IMarker.BOOKMARK);
		IResource child = ((IProject) resource).members()[0];
		markers[1] = child.createMarker(IMarker.TASK);
		listener.reset();
		resource.move(destination, false, createTestMonitor());
		IResource destinationResource = getWorkspace().getRoot().findMember(destination);
		markers[2] = destinationResource.getMarker(markers[0].getId());
		IResource destinationChild = ((IProject) destinationResource).findMember(child.getName());
		markers[3] = destinationChild.getMarker(markers[1].getId());
		listener.assertNumberOfAffectedResources(4);
		listener.assertChanges(resource, null, new IMarker[] { markers[0] }, null);
		listener.assertChanges(child, null, new IMarker[] { markers[1] }, null);
		listener.assertChanges(destinationResource, new IMarker[] { markers[2] }, null, null);
		listener.assertChanges(destinationChild, new IMarker[] { markers[3] }, null, null);

		// COPY the resource and look at the deltas -
		// there should be no changes since markers are not copied
		resource = getWorkspace().getRoot().findMember("2");
		destination = resource.getFullPath().removeLastSegments(1)
				.append(resource.getFullPath().lastSegment() + "copy");
		resource.createMarker(IMarker.BOOKMARK);
		listener.reset();
		resource.copy(destination, false, createTestMonitor());
		listener.assertNumberOfAffectedResources(0);

		// delete all markers for a clean run next time
		getWorkspace().getRoot().deleteMarkers(null, true, IResource.DEPTH_INFINITE);
	}

	/**
	 * Tests whether markers correctly copy with resources.
	 */
	@Test
	public void testCopyResource() {
	}

	@Test
	public void testCreateMarker() throws CoreException {
		// create markers on our hierarchy of resources
		for (IResource resource : resources) {
			// Create and register a listener.
			MarkersChangeListener listener = new MarkersChangeListener();
			setResourceChangeListener(listener);

			IMarker[] markers = new IMarker[3];
			markers[0] = resource.createMarker(IMarker.PROBLEM);
			markers[1] = resource.createMarker(IMarker.BOOKMARK);
			markers[2] = resource.createMarker(IMarker.TASK);
			assertMarkersExist(markers);
			listener.assertNumberOfAffectedResources(1);
			listener.assertChanges(resource, markers, null, null);
			assertThrows(resource.getFullPath().toString(), RuntimeException.class, () -> resource.createMarker(null));
		}

		// try creating a marker on a resource which does't exist
		IResource testResource = getWorkspace().getRoot().getFile(IPath.fromOSString("non/existant/resource"));
		assertFalse("resource should not exist: " + testResource, testResource.exists());
		assertThrows(testResource.getFullPath().toString(), CoreException.class,
				() -> testResource.createMarker(IMarker.PROBLEM));
	}

	@Test
	public void testCreateMarkerWithAttributes() throws CoreException {
		// Create and register a listener.
		MarkersChangeListener listener = new MarkersChangeListener();
		setResourceChangeListener(listener);

		// create markers on our hierarchy of resources
		for (IResource resource : resources) {
			listener.reset();

			IMarker[] markers = new IMarker[] { //
					resource.createMarker(IMarker.PROBLEM, null), //
					resource.createMarker(IMarker.BOOKMARK, Collections.emptyMap()),
					resource.createMarker(IMarker.TASK, Map.of(IMarker.MESSAGE, "My text")) };
			assertMarkersExist(markers);
			listener.assertNumberOfAffectedResources(1);
			listener.assertChanges(resource, markers, null, null);
		}
	}

	@Test
	public void testCreateNullMarkerWithAttributesShouldFail() {
		// create markers on our hierarchy of resources
		for (IResource resource : resources) {
			assertThrows(RuntimeException.class, () -> resource.createMarker(null, null));
		}
	}

	@Test
	public void testCreateMarkerWithAttributesOnAResourceWhichDoesNotExistShouldFail() {
		// try creating a marker on a resource which does't exist
		IResource testResource = getWorkspace().getRoot().getFile(IPath.fromOSString("non/existant/resource"));
		assertFalse("resource should not exist: " + testResource, testResource.exists());
		assertThrows(testResource.getFullPath().toString(), CoreException.class,
				() -> testResource.createMarker(IMarker.PROBLEM, Map.of(IMarker.MESSAGE, "My text")));
	}

	// testing that markers creation and calling setAttribute trigger multiple
	// resource change
	// events (which is bad for performance hence the better createMarker(String
	// type, Map<String, Object> attributes) method
	@Test
	public void testThatSettingAttributesTriggerAdditionalResourceChangeEvent() throws CoreException {
		// Create and register a listener.
		MarkersNumberOfDeltasChangeListener listener = new MarkersNumberOfDeltasChangeListener();
		getWorkspace().addResourceChangeListener(listener);
		for (IResource resource : resources) {
			listener.reset();
			// each setAttributes triggers one additional resource change event
			IMarker marker = resource.createMarker(TEST_PROBLEM_MARKER);
			marker.setAttribute(IMarker.MESSAGE, createRandomString());
			marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
			assertThat(listener.numberOfChanges(), is(3));
		}
	}

	// testing that markers creation with attributes
	@Test
	public void testThatMarkersWithAttributesOnlyTriggerOnResourceChangeEvent() throws CoreException {
		// Create and register a listener.
		MarkersNumberOfDeltasChangeListener listener = new MarkersNumberOfDeltasChangeListener();
		getWorkspace().addResourceChangeListener(listener);
		for (IResource resource : resources) {
			listener.reset();
			// createMarker with attributes triggers only one resource change event
			listener.reset();
			// each setAttributes triggers one resource change event
			resource.createMarker(TEST_PROBLEM_MARKER,
					Map.of(IMarker.MESSAGE, createRandomString(), IMarker.PRIORITY, IMarker.PRIORITY_HIGH));
			assertThat(listener.numberOfChanges(), is(1));
		}
	}

	@Test
	public void testCreationTime() throws CoreException {
		for (IResource element : resources) {
			IMarker marker = element.createMarker(IMarker.PROBLEM);
			assertThat("creation time for marker in resource " + element.getFullPath() + " is not set",
					marker.getCreationTime(), not(is(0)));
		}
	}

	@Test
	public void testDeleteMarker() throws CoreException {
		IMarker marker = null;

		// Create and register a listener.
		MarkersChangeListener listener = new MarkersChangeListener();
		setResourceChangeListener(listener);

		// for each resource in the hierarchy do...
		for (IResource resource : resources) {
			// create the marker, assert that it exists, and then remove it
			listener.reset();
			marker = resource.createMarker(IMarker.PROBLEM);
			listener.assertNumberOfAffectedResources(1);
			listener.assertChanges(resource, new IMarker[] { marker }, null, null);
			assertMarkerExists(marker);

			listener.reset();
			marker.delete();
			assertMarkerDoesNotExist(marker);
			listener.assertNumberOfAffectedResources(1);
			listener.assertChanges(resource, null, new IMarker[] { marker }, null);

			// Check that a non-existant marker can be removed
			marker.delete();

			// create multiple markers, assert they exist, and then remove them.
			IMarker[] markers = new IMarker[3];
			markers[0] = resource.createMarker(IMarker.BOOKMARK);
			assertMarkerExists(markers[0]);
			markers[1] = resource.createMarker(IMarker.TASK);
			assertMarkerExists(markers[1]);
			markers[2] = resource.createMarker(IMarker.PROBLEM);
			assertMarkerExists(markers[2]);

			listener.reset();
			getWorkspace().deleteMarkers(markers);
			listener.assertNumberOfAffectedResources(1);
			listener.assertChanges(resource, null, markers, null);
			assertMarkersDoNotExist(markers);
		}
	}

	@Test
	public void testDeleteMarkers() throws CoreException {
		IMarker[] markers = createMarkers(resources, IMarker.PROBLEM);

		// Check that a collection of markers can be removed.
		getWorkspace().deleteMarkers(markers);
		assertMarkersDoNotExist(markers);

		// Check that an empty collection of markers can be removed.
		getWorkspace().deleteMarkers(new IMarker[0]);
	}

	@Test
	public void testFindMarkers() throws CoreException {
		// test finding some markers which actually exist
		IMarker[] markers = createMarkers(resources, IMarker.PROBLEM);
		IMarker[] found = getWorkspace().getRoot().findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_INFINITE);
		assertThat(found, arrayContainingInAnyOrder(markers));
		found = getWorkspace().getRoot().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		assertThat(found, arrayContainingInAnyOrder(markers));
		// test finding some markers which don't exist
		found = getWorkspace().getRoot().findMarkers(IMarker.BOOKMARK, false, IResource.DEPTH_INFINITE);
		assertThat(found, arrayWithSize(0));

		// add more markers and do a search on all marker types
		Vector<IMarker> allMarkers = new Vector<>(markers.length * 3);
		Collections.addAll(allMarkers, markers);
		markers = createMarkers(resources, IMarker.BOOKMARK);
		Collections.addAll(allMarkers, markers);
		markers = createMarkers(resources, IMarker.TASK);
		Collections.addAll(allMarkers, markers);
		found = getWorkspace().getRoot().findMarkers(null, false, IResource.DEPTH_INFINITE);
		assertThat(found, arrayContainingInAnyOrder(allMarkers.toArray(new IMarker[allMarkers.size()])));
		found = getWorkspace().getRoot().findMarkers(IMarker.MARKER, true, IResource.DEPTH_INFINITE);
		assertThat(found, arrayContainingInAnyOrder(allMarkers.toArray(new IMarker[allMarkers.size()])));
	}

	/*
	 * Bug 35300 - ClassCastException if marker transient attribute is set to a non-boolean
	 */
	@Test
	public void test_35300() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		createInWorkspace(project);
		String MARKER_ID = "foomarker.example.com";
		int expected = 4;

		// setup
		IMarker marker = project.createMarker(MARKER_ID);
		marker.setAttribute(IMarker.TRANSIENT, expected);

		int actual = marker.getAttribute(IMarker.TRANSIENT, -1);
		assertThat(actual, is(expected));
		marker.setAttribute(IMarker.MESSAGE, createRandomString());
	}

	@Test
	public void test_10989() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		createInWorkspace(project);
		IFile file = project.getFile("foo.txt");
		file.create(createRandomContentsStream(), true, null);
		file.createMarker(IMarker.PROBLEM);
		IMarker[] found = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
		assertThat(found, arrayWithSize(1));
		found = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		assertThat(found, arrayWithSize(1));
		project.delete(true, true, null);
	}

	/*
	 * Bug 289811 - ArrayIndexOutOfBoundsException in MarkerAttributeMap
	 */
	@Test
	public void test_289811() throws CoreException {
		String testValue = createRandomString();
		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		project.create(null);
		project.open(null);
		IFile file = project.getFile("foo.txt");
		file.create(createRandomContentsStream(), true, null);
		IMarker marker = file.createMarker(IMarker.PROBLEM);
		marker.setAttributes(new HashMap<>());
		marker.setAttribute(IMarker.SEVERITY, testValue);
		assertMarkerHasAttributeValue(marker, IMarker.SEVERITY, testValue);
		project.delete(true, true, null);
	}

	/**
	 * Tests public API method IResource#findMaxProblemSeverity
	 */
	@Test
	public void testFindMaxProblemSeverity() throws CoreException {
		final IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("testFindMaxProblemSeverity");
		IFolder folder = project.getFolder("top");
		IFolder sub = folder.getFolder("sub");
		IFile topFile = folder.getFile("a.txt");
		IFile subFile = sub.getFile("b.txt");
		IResource[] allResources = new IResource[] {project, folder, sub, topFile, subFile};
		createInWorkspace(allResources);

		assertThat(root.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE), is(-1));
		assertThat(root.findMaxProblemSeverity(IMarker.TASK, true, IResource.DEPTH_INFINITE), is(-1));
		assertThat(root.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ZERO), is(-1));

		createProblem(subFile, IMarker.SEVERITY_INFO);
		assertThat(root.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE),
				is(IMarker.SEVERITY_INFO));
		assertThat(root.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ONE), is(-1));
		assertThat(root.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ZERO), is(-1));
		assertThat(root.findMaxProblemSeverity(IMarker.TASK, true, IResource.DEPTH_INFINITE), is(-1));

		assertThat(folder.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE),
				is(IMarker.SEVERITY_INFO));
		assertThat(folder.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ONE), is(-1));
		assertThat(folder.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ZERO), is(-1));
		assertThat(folder.findMaxProblemSeverity(IMarker.TASK, true, IResource.DEPTH_INFINITE), is(-1));

		assertThat(sub.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE),
				is(IMarker.SEVERITY_INFO));
		assertThat(sub.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ONE), is(IMarker.SEVERITY_INFO));
		assertThat(sub.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ZERO), is(-1));
		assertThat(sub.findMaxProblemSeverity(IMarker.TASK, true, IResource.DEPTH_INFINITE), is(-1));

		assertThat(subFile.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE),
				is(IMarker.SEVERITY_INFO));
		assertThat(subFile.findMaxProblemSeverity(IMarker.PROBLEM, false, IResource.DEPTH_ONE), is(IMarker.SEVERITY_INFO));
		assertThat(subFile.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ZERO),
				is(IMarker.SEVERITY_INFO));
		assertThat(subFile.findMaxProblemSeverity(IMarker.TASK, true, IResource.DEPTH_INFINITE), is(-1));

		createProblem(topFile, IMarker.SEVERITY_ERROR);
		assertThat(root.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE),
				is(IMarker.SEVERITY_ERROR));
		assertThat(folder.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ONE),
				is(IMarker.SEVERITY_ERROR));
		assertThat(topFile.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ONE),
				is(IMarker.SEVERITY_ERROR));
		assertThat(sub.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ONE), is(IMarker.SEVERITY_INFO));
		assertThat(subFile.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ZERO),
				is(IMarker.SEVERITY_INFO));
	}

	/**
	 * Tests public API method IMarker#isSubTypeOf
	 */
	@Test
	public void testIsSubTypeOf() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("testisSubType");
		IMarker marker, task, problem, testProblem, invalid;

		final String INVALID_MARKER = "does.not.exist.at.AllMarker";

		createInWorkspace(project);
		marker = project.createMarker(IMarker.MARKER);
		task = project.createMarker(IMarker.TASK);
		problem = project.createMarker(IMarker.PROBLEM);
		testProblem = project.createMarker(TEST_PROBLEM_MARKER);
		invalid = project.createMarker(INVALID_MARKER);

		assertMarkerIsSubtype(marker, IMarker.MARKER);
		assertMarkerIsNoSubtype(marker, IMarker.TASK);
		assertMarkerIsNoSubtype(marker, IMarker.PROBLEM);
		assertMarkerIsNoSubtype(marker, TEST_PROBLEM_MARKER);
		assertMarkerIsNoSubtype(marker, INVALID_MARKER);

		assertMarkerIsSubtype(task, IMarker.MARKER);
		assertMarkerIsSubtype(task, IMarker.TASK);
		assertMarkerIsNoSubtype(task, IMarker.PROBLEM);
		assertMarkerIsNoSubtype(task, TEST_PROBLEM_MARKER);
		assertMarkerIsNoSubtype(task, INVALID_MARKER);

		assertMarkerIsSubtype(problem, IMarker.MARKER);
		assertMarkerIsNoSubtype(problem, IMarker.TASK);
		assertMarkerIsSubtype(problem, IMarker.PROBLEM);
		assertMarkerIsNoSubtype(problem, TEST_PROBLEM_MARKER);
		assertMarkerIsNoSubtype(problem, INVALID_MARKER);

		assertMarkerIsSubtype(testProblem, IMarker.MARKER);
		assertMarkerIsNoSubtype(testProblem, IMarker.TASK);
		assertMarkerIsSubtype(testProblem, IMarker.PROBLEM);
		assertMarkerIsSubtype(testProblem, TEST_PROBLEM_MARKER);
		assertMarkerIsNoSubtype(testProblem, INVALID_MARKER);

		// behaviour with an undefined marker type is not specified, but
		// test current behaviour to give us advance warning of accidental
		// behavioural change
		assertMarkerIsNoSubtype(invalid, IMarker.MARKER);
		assertMarkerIsNoSubtype(invalid, IMarker.TASK);
		assertMarkerIsNoSubtype(invalid, IMarker.PROBLEM);
		assertMarkerIsNoSubtype(invalid, TEST_PROBLEM_MARKER);
		assertMarkerIsSubtype(invalid, INVALID_MARKER);
	}

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 */
	@Test
	public void testMarkerChangesInDelta() throws CoreException {
		// Create and register a listener.
		MarkersChangeListener listener = new MarkersChangeListener();
		setResourceChangeListener(listener);

		for (IResource resource : resources) {
			IMarker[] markers = new IMarker[3];

			// ADD a marker
			listener.reset();
			markers[0] = resource.createMarker(IMarker.PROBLEM);
			assertMarkerExists(markers[0]);
			listener.assertNumberOfAffectedResources(1);
			listener.assertChanges(resource, new IMarker[] { markers[0] }, null, null);

			// ADD more markers to the same resource
			listener.reset();
			markers[1] = resource.createMarker(IMarker.BOOKMARK);
			markers[2] = resource.createMarker(IMarker.TASK);
			assertMarkersExist(new IMarker[] { markers[1], markers[2] });
			listener.assertNumberOfAffectedResources(1);
			listener.assertChanges(resource, new IMarker[] { markers[1], markers[2] }, null, null);

			// CHANGE a marker
			listener.reset();
			markers[0].setAttribute(IMarker.MESSAGE, "My text.");
			listener.assertNumberOfAffectedResources(1);
			listener.assertChanges(resource, null, null, new IMarker[] { markers[0] });

			// CHANGE more markers
			listener.reset();
			markers[1].setAttribute(IMarker.SEVERITY, "Low");
			markers[2].setAttribute(IMarker.PRIORITY, "Normal");
			listener.assertNumberOfAffectedResources(1);
			listener.assertChanges(resource, null, null, new IMarker[] { markers[1], markers[2] });

			// DELETE a marker
			listener.reset();
			markers[0].delete();
			assertMarkerDoesNotExist(markers[0]);
			listener.assertNumberOfAffectedResources(1);
			listener.assertChanges(resource, null, new IMarker[] { markers[0] }, null);

			// DELETE more markers
			listener.reset();
			resource.deleteMarkers(null, false, IResource.DEPTH_ZERO);
			assertMarkersDoNotExist(new IMarker[] { markers[1], markers[2] });
			listener.assertNumberOfAffectedResources(1);
			listener.assertChanges(resource, null, new IMarker[] { markers[1], markers[2] }, null);

			// ADD, REMOVE and CHANGE markers
			markers[0] = resource.createMarker(IMarker.PROBLEM);
			markers[1] = resource.createMarker(IMarker.BOOKMARK);
			listener.reset();
			markers[0].delete();
			assertMarkerDoesNotExist(markers[0]);
			markers[1].setAttribute(IMarker.MESSAGE, createRandomString());
			markers[2] = resource.createMarker(IMarker.TASK);
			listener.assertNumberOfAffectedResources(1);
			listener.assertChanges(resource, new IMarker[] { markers[2] }, new IMarker[] { markers[0] },
					new IMarker[] { markers[1] });

		}

		// DELETE the resource and see what the marker delta is
		IResource resource = getWorkspace().getRoot();
		resource.deleteMarkers(null, true, IResource.DEPTH_INFINITE);
		IMarker marker = resource.createMarker(IMarker.BOOKMARK);
		listener.reset();
		resource.delete(true, createTestMonitor());
		assertMarkerDoesNotExist(marker);
		listener.assertChanges(resource, null, new IMarker[] { marker }, null);
	}

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 * Particularly, checks that the MarkerDelta attributes reflect the
	 * state of the marker before the change occurred.
	 */
	@Test
	public void testMarkerDeltaAttributes() throws CoreException {
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
		getWorkspace().run(body, createTestMonitor());

		//create the attribute change listener
		MarkerAttributeChangeListener listener = new MarkerAttributeChangeListener();
		getWorkspace().addResourceChangeListener(listener);

		// add a new attribute
		listener.expectChanges(markers[0]);
		markers[0].setAttribute(IMarker.MESSAGE, "Message");
		listener.verifyChanges();

		// change an attribute
		listener.expectChanges(markers[0]);
		markers[0].setAttribute(IMarker.MESSAGE, "NewMessage");
		listener.verifyChanges();

		// remove an attribute
		listener.expectChanges(markers[0]);
		markers[0].setAttribute(IMarker.MESSAGE, null);
		listener.verifyChanges();

		// add attribute to marker that already has attributes
		listener.expectChanges(markers[2]);
		markers[2].setAttribute(IMarker.CHAR_END, 5);
		listener.verifyChanges();

		// add+change
		listener.expectChanges(markers[1]);
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			markers[1].setAttribute(IMarker.CHAR_START, 5);
			markers[1].setAttribute(IMarker.CHAR_END, 10);
		}, createTestMonitor());
		listener.verifyChanges();

		// change+remove same marker
		listener.expectChanges(markers[1]);
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			markers[1].setAttribute(IMarker.CHAR_START, 5);
			markers[1].setAttribute(IMarker.CHAR_START, null);
		}, createTestMonitor());
		listener.verifyChanges();

		// change multiple markers
		listener.expectChanges(markers);
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			markers[0].setAttribute(IMarker.CHAR_START, 5);
			markers[1].setAttribute(IMarker.CHAR_START, 10);
			markers[2].setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_LOW);
		}, createTestMonitor());
		listener.verifyChanges();
	}

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 */
	@Test
	public void testMarkerDeltasCopyResource() throws CoreException {
		// Create and register a listener.
		final MarkersChangeListener listener = new MarkersChangeListener();
		setResourceChangeListener(listener);

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
		getWorkspace().run(body, createTestMonitor());
		listener.reset();

		// copy all non-project resources
		IProject[] projects = getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			IResource[] children = project.members();
			for (IResource element : children) {
				IPath destination = IPath.fromOSString(element.getName() + "copy");
				element.copy(destination, true, createTestMonitor());
			}
		}

		listener.assertNumberOfAffectedResources(0);
	}

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 */
	@Test
	public void testMarkerDeltasMerge() throws CoreException {
		// Create and register a listener.
		final MarkersChangeListener listener = new MarkersChangeListener();
		setResourceChangeListener(listener);

		for (final IResource resource : resources) {
			// ADD + REMOVE = nothing
			IWorkspaceRunnable addAndRemoveOperation = monitor -> {
				listener.reset();
				IMarker marker = resource.createMarker(IMarker.PROBLEM);
				assertMarkerExists(marker);
				marker.delete();
				assertMarkerDoesNotExist(marker);
			};
			getWorkspace().run(addAndRemoveOperation, createTestMonitor());
			listener.assertNumberOfAffectedResources(0);
			listener.assertChanges(resource, null, null, null);

			// ADD + CHANGE = ADD
			// cannot re-assign variable value within the code below, so must
			// put our marker value inside an array and set the element.
			AtomicReference<IMarker> addAndChangeMarker = new AtomicReference<>();
			IWorkspaceRunnable addAndChangeOperation = monitor -> {
				listener.reset();
				addAndChangeMarker.set(resource.createMarker(IMarker.PROBLEM));
				assertMarkerExists(addAndChangeMarker.get());
				addAndChangeMarker.get().setAttribute(IMarker.MESSAGE, "my message text");
				assertMarkerHasAttributeValue(addAndChangeMarker.get(), IMarker.MESSAGE, "my message text");
			};
			getWorkspace().run(addAndChangeOperation, createTestMonitor());
			listener.assertNumberOfAffectedResources(1);
			listener.assertChanges(resource, new IMarker[] { addAndChangeMarker.get() }, null, null);

			// TODO
			// REMOVE + ADD = CHANGE
			// try {
			// final IMarker[] markers = new IMarker[1];
			// markers[0] = resource.createMarker(IMarker.PROBLEM);
			// assertExists("3.0." + resource.getFullPath(), markers[0]);
			// IWorkspaceRunnable body = new IWorkspaceRunnable() {
			// public void run(IProgressMonitor monitor) throws CoreException {
			// listener.reset();
			// markers[0].delete();
			// assertDoesNotExist("3.1." + resource.getFullPath(), markers[0]);
			// markers[0] = resource.createMarker(IMarker.PROBLEM);
			// assertExists("3.2." + resource.getFullPath(), markers[0]);
			// }
			// };
			// getWorkspace().run(body, getMonitor());
			// assertEquals("3.3." + resource.getFullPath(), 1,
			// listener.numAffectedResources());
			// assert("3.4." + resource.getFullPath(), listener.checkChanges(resource, null,
			// null, new IMarker[] { markers[0] }));
			// } catch (CoreException e) {
			// fail("3.99." + resource.getFullPath(), e);
			// }

			// CHANGE + CHANGE = CHANGE

			IMarker changeAndChangeMarker = resource.createMarker(IMarker.PROBLEM);
			assertMarkerExists(changeAndChangeMarker);
			IWorkspaceRunnable changeAndChangeOperation = monitor -> {
				listener.reset();
				changeAndChangeMarker.setAttribute(IMarker.MESSAGE, "my message text");
				assertMarkerHasAttributeValue(changeAndChangeMarker, IMarker.MESSAGE, "my message text");
				changeAndChangeMarker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
				assertMarkerHasAttributeValue(changeAndChangeMarker, IMarker.PRIORITY,
						IMarker.PRIORITY_HIGH);
			};
			getWorkspace().run(changeAndChangeOperation, createTestMonitor());
			listener.assertNumberOfAffectedResources(1);
			listener.assertChanges(resource, null, null, new IMarker[] { changeAndChangeMarker });

			// CHANGE + REMOVE = REMOVE
			IMarker changeAndRemoveMarker = resource.createMarker(IMarker.PROBLEM);
			assertMarkerExists(changeAndRemoveMarker);
			IWorkspaceRunnable changeAndRemoveOperation = monitor -> {
				listener.reset();
				changeAndRemoveMarker.setAttribute(IMarker.MESSAGE, "my message text");
				assertMarkerHasAttributeValue(changeAndRemoveMarker, IMarker.MESSAGE, "my message text");
				changeAndRemoveMarker.delete();
				assertMarkerDoesNotExist(changeAndRemoveMarker);
			};
			getWorkspace().run(changeAndRemoveOperation, createTestMonitor());
			listener.assertNumberOfAffectedResources(1);
			listener.assertChanges(resource, null, new IMarker[] { changeAndRemoveMarker }, null);

			// cleanup after each iteration
			resource.deleteMarkers(null, true, IResource.DEPTH_ZERO);
		}
	}

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 */
	@Test
	public void testMarkerDeltasMoveFolder() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		final IProject project = root.getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subFile.txt");
		createInWorkspace(new IResource[] { project, folder, file, subFile });
		waitForEncodingRelatedJobs(testName.getMethodName());
		IFolder destFolder = project.getFolder("myOtherFolder");
		IFile destSubFile = destFolder.getFile(subFile.getName());

		// Create and register a listener.
		MarkersChangeListener listener = new MarkersChangeListener();
		setResourceChangeListener(listener);

		// create markers on the resources
		IMarker folderMarker = folder.createMarker(IMarker.BOOKMARK);
		IMarker subFileMarker = subFile.createMarker(IMarker.BOOKMARK);

		listener = new MarkersChangeListener();
		setResourceChangeListener(listener);
		// move the files
		folder.move(destFolder.getFullPath(), IResource.FORCE, createTestMonitor());

		// verify marker deltas
		listener.assertChanges(folder, null, new IMarker[] { folderMarker }, null);
		IMarker[] folderMarkers = destFolder.findMarkers(null, true, IResource.DEPTH_ZERO);
		assertSingleMarkerWithId(folderMarkers, folderMarker.getId());
		listener.assertChanges(destFolder, new IMarker[] { folderMarkers[0] }, null, null);

		listener.assertChanges(subFile, null, new IMarker[] { subFileMarker }, null);
		IMarker[] subFolderMarkers = destSubFile.findMarkers(null, true, IResource.DEPTH_ZERO);
		assertSingleMarkerWithId(subFolderMarkers, subFileMarker.getId());
		listener.assertChanges(destSubFile, new IMarker[] { subFolderMarkers[0] }, null, null);
	}

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 */
	@Test
	public void testMarkerDeltasMoveFile() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		final IProject project = root.getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subFile.txt");
		createInWorkspace(new IResource[] { project, folder, file, subFile });
		waitForEncodingRelatedJobs(testName.getMethodName());
		IFile destFile = folder.getFile(file.getName());
		IFile destSubFile = project.getFile(subFile.getName());

		// Create and register a listener.
		final MarkersChangeListener listener = new MarkersChangeListener();
		setResourceChangeListener(listener);

		// create markers on the resources
		IMarker fileMarker = file.createMarker(IMarker.BOOKMARK);
		IMarker subFileMarker = subFile.createMarker(IMarker.BOOKMARK);
		listener.reset();

		// move the files
		file.move(destFile.getFullPath(), IResource.FORCE, createTestMonitor());
		subFile.move(destSubFile.getFullPath(), IResource.FORCE, createTestMonitor());

		// verify marker deltas
		listener.assertChanges(file, null, new IMarker[] { fileMarker }, null);
		IMarker[] markers = destFile.findMarkers(null, true, IResource.DEPTH_ZERO);
		assertSingleMarkerWithId(markers, fileMarker.getId());
		listener.assertChanges(destFile, new IMarker[] { markers[0] }, null, null);

		listener.assertChanges(subFile, null, new IMarker[] { subFileMarker }, null);
		markers = destSubFile.findMarkers(null, true, IResource.DEPTH_ZERO);
		assertSingleMarkerWithId(markers, subFileMarker.getId());
		listener.assertChanges(destSubFile, new IMarker[] { markers[0] }, null, null);
	}

	/**
	 * Tests the appearance of marker changes in the resource delta.
	 */
	@Test
	public void testMarkerDeltasMoveProject() throws CoreException {
		// Create and register a listener.
		final MarkersChangeListener listener = new MarkersChangeListener();
		setResourceChangeListener(listener);

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
		getWorkspace().run(body, createTestMonitor());
		listener.reset();

		// move all resources
		IProject[] projects = getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			IPath destination = IPath.fromOSString(project.getName() + "move");
			project.move(destination, true, createTestMonitor());
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
			assertThat(marker, not(is(nullValue())));
			listener.assertChanges(oldResource, null, new IMarker[] { marker }, null);
			IMarker[] markers = resource.findMarkers(null, true, IResource.DEPTH_ZERO);
			assertSingleMarkerWithId(markers, marker.getId());
			listener.assertChanges(resource, new IMarker[] { markers[0] }, null, null);
			return true;
		};
		listener.assertNumberOfAffectedResources(count[0] * 2);
		getWorkspace().getRoot().accept(visitor);
	}

	@Test
	public void testMarkerSave() throws Exception {
		IMarker[] newMarkers = createMarkers(resources, IMarker.PROBLEM);
		IMarker[] expected = new IMarker[newMarkers.length * 3];
		System.arraycopy(newMarkers, 0, expected, 0, newMarkers.length);
		newMarkers = createMarkers(resources, IMarker.BOOKMARK);
		System.arraycopy(newMarkers, 0, expected, newMarkers.length, newMarkers.length);
		newMarkers = createMarkers(resources, IMarker.TASK);
		System.arraycopy(newMarkers, 0, expected, newMarkers.length * 2, newMarkers.length);

		final MarkerManager manager = ((Workspace) getWorkspace()).getMarkerManager();

		// write all the markers to the output stream
		File file = Platform.getLocation().append(".testmarkers").toFile();
		try (OutputStream fileOutput = new FileOutputStream(file)) {
			try (DataOutputStream output = new DataOutputStream(fileOutput)) {
				final List<String> list = new ArrayList<>(5);
				IResourceVisitor visitor = resource -> {
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
					try {
						manager.save(info, requestor, output, list);
					} catch (IOException e) {
						throw new IllegalStateException("saving failed", e);
					}
					return true;
				};
				getWorkspace().getRoot().accept(visitor);
			}
		}

		// delete all markers resources
		getWorkspace().getRoot().deleteMarkers(null, true, IResource.DEPTH_INFINITE);
		assertMarkersDoNotExist(expected);

		// read in the markers from the file
		try (InputStream fileInput = new FileInputStream(file)) {
			try (DataInputStream input = new DataInputStream(fileInput)) {
				IWorkspaceRunnable body = monitor -> {
					MarkerReader reader = new MarkerReader((Workspace) getWorkspace());
					try {
						reader.read(input, true);
					} catch (IOException e) {
						throw new IllegalStateException("failed reading markers", e);
					}
				};
				getWorkspace().run(body, createTestMonitor());
			}
		}

		// assert that the markers retrieved are the same as the ones we used
		// to have
		assertMarkersExist(expected);
		IMarker[] actual = getWorkspace().getRoot().findMarkers(null, false, IResource.DEPTH_INFINITE);
		assertThat(actual, arrayContainingInAnyOrder(expected));

		// cleanup
		assertTrue("deleting file failed", file.delete());
	}

	@Test
	public void testMarkerSaveTransient() throws Exception {
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
		getWorkspace().getRoot().accept(visitor);

		final MarkerManager manager = ((Workspace) getWorkspace()).getMarkerManager();
		IMarker[] expected = persistentMarkers.toArray(new IMarker[persistentMarkers.size()]);

		// write all the markers to the output stream
		File file = Platform.getLocation().append(".testmarkers").toFile();
		try(OutputStream fileOutput = new FileOutputStream(file)) {
			try(DataOutputStream output = new DataOutputStream(fileOutput)) {
				final List<String> list = new ArrayList<>(5);
				visitor = resource -> {
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
					try {
						manager.save(info, requestor, output, list);
					} catch (IOException e) {
						throw new IllegalStateException("saving failed", e);
					}
					return true;
				};
				getWorkspace().getRoot().accept(visitor);
			}
		}

		// delete all markers resources
		getWorkspace().getRoot().deleteMarkers(null, true, IResource.DEPTH_INFINITE);
		assertMarkersDoNotExist(expected);

		// read in the markers from the file
		try (InputStream fileInput = new FileInputStream(file)) {
			try (DataInputStream input = new DataInputStream(fileInput)) {
				IWorkspaceRunnable body = monitor -> {
					MarkerReader reader = new MarkerReader((Workspace) getWorkspace());
					try {
						reader.read(input, true);
					} catch (IOException e) {
						throw new IllegalStateException("Failed reading markers", e);
					}
				};
				getWorkspace().run(body, createTestMonitor());
			}
		}

		// assert that the markers retrieved are the same as the ones we used
		// to have
		assertMarkersExist(expected);
		IMarker[] actual = getWorkspace().getRoot().findMarkers(null, false, IResource.DEPTH_INFINITE);
		assertThat(actual, arrayContainingInAnyOrder(expected));

		// cleanup
		assertTrue("deleting file failed", file.delete());
	}

	/**
	 * Tests whether markers correctly move with resources.
	 */
	@Test
	public void testMoveResource() {
	}

	/*
	 * Test for PR: "1FWT3V5: ITPCORE:WINNT - Task view shows entries for closed projects"
	 */
	@Test
	public void testProjectCloseOpen() throws CoreException {
		// create a marker on the project
		IProject project = getWorkspace().getRoot().getProjects()[0];
		IMarker marker = project.createMarker(IMarker.BOOKMARK);
		assertMarkerExists(marker);

		project.close(createTestMonitor());
		assertMarkerDoesNotExist(marker);

		project.open(createTestMonitor());
		assertMarkerExists(marker);
	}

	@Test
	public void testSetGetAttribute() throws CoreException {
		for (IResource resource : resources) {
			String resourcePath = resource.getFullPath().toString();

			// getting a non-existant attribute should return null
			IMarker marker = resource.createMarker(IMarker.PROBLEM);
			assertMarkerHasAttributeValue(marker, IMarker.MESSAGE, null);

			// set an attribute, get its value, then remove it
			String testMessage = createRandomString();
			marker.setAttribute(IMarker.MESSAGE, testMessage);
			assertMarkerHasAttributeValue(marker, IMarker.MESSAGE, testMessage);
			marker.setAttribute(IMarker.MESSAGE, null);
			assertMarkerHasAttributeValue(marker, IMarker.MESSAGE, null);

			// set more attributes, get their values, then remove one
			String[] keys = new String[] { IMarker.LOCATION, IMarker.SEVERITY, IMarker.DONE };
			Object[] values = new Object[3];
			values[0] = createRandomString();
			values[1] = Integer.valueOf(5);
			values[2] = Boolean.FALSE;
			Map<String, Object> originalMap = Map.of(keys[0], values[0], keys[1], values[1], keys[2], values[2]);
			marker.setAttributes(keys, values);
			Object[] found = marker.getAttributes(keys);
			assertThat(resourcePath, found, is(values));
			marker.setAttribute(IMarker.SEVERITY, null);
			values[1] = null;
			found = marker.getAttributes(keys);
			assertThat(resourcePath, found, is(values));
			values[1] = Integer.valueOf(5);
			marker.setAttribute(IMarker.SEVERITY, values[1]);
			assertThat(resourcePath, marker.getAttributes(), is(originalMap));

			// try sending null as args
			assertThrows(resourcePath, RuntimeException.class, () -> marker.getAttribute(null));
			assertThrows(resourcePath, RuntimeException.class, () -> marker.getAttributes(null));
			assertThrows(resourcePath, RuntimeException.class, () -> marker.setAttribute(null, createRandomString()));
			assertThrows(resourcePath, RuntimeException.class,
					() -> marker.setAttributes(null, new String[] { createRandomString() }));
			assertThrows(resourcePath, RuntimeException.class,
					() -> marker.setAttributes(new String[] { IMarker.MESSAGE }, null));
			//set attributes on deleted marker
			marker.delete();
			assertThrows(resourcePath, CoreException.class, () -> marker.setAttribute(IMarker.MESSAGE, "Hello"));
			assertThrows(resourcePath, CoreException.class,
					() -> marker.setAttributes(new String[] { IMarker.LINE_NUMBER },
					new Object[] { Integer.valueOf(4) }));
			HashMap<String, String> attributes = new HashMap<>();
			attributes.put(IMarker.MESSAGE, "Hello");
			assertThrows(resourcePath, CoreException.class, () -> marker.setAttributes(attributes));
		}
	}

	@Test
	public void testGetAttributesEquality() throws Exception {
		final String value = "Some value";
		for (int i = 0; i < resources.length; i++) {
			IMarker marker = resources[i].createMarker(IMarker.PROBLEM, Map.of(String.valueOf(i), value));

			// Check we can get the value by equal key
			assertMarkerHasAttributeValue(marker, String.valueOf(i), value);

			// Check the map returned by marker is equal to equal map
			Map<String, Object> existing = marker.getAttributes();
			Map<String, Object> otherAttributes = Map.of(String.valueOf(i), value);
			assertThat(otherAttributes, is(existing));
		}
	}

	@Test
	public void testSetGetAttribute2() throws CoreException {
		for (IResource resource : resources) {
			String resourcePath = resource.getFullPath().toString();

			// getting a non-existant attribute should return null or the specified default
			IMarker marker = resource.createMarker(IMarker.PROBLEM);
			// #getAttribute(Object)
			assertThat(resourcePath, marker.getAttribute(IMarker.MESSAGE), is(nullValue()));
			// #getAttribute(String, String)
			assertThat(resourcePath, marker.getAttribute(IMarker.MESSAGE, "default"), is("default"));
			// #getAttribute(String, boolean)
			assertThat(resourcePath, marker.getAttribute(IMarker.MESSAGE, true), is(true));
			// #getAttribute(String, int)
			assertThat(resourcePath, marker.getAttribute(IMarker.MESSAGE, 5), is(5));
			// #getAttributes()
			assertThat(resourcePath, marker.getAttributes(), is(nullValue()));
			// #getAttributes(String[])
			assertThat(resourcePath, marker.getAttributes(new String[] { IMarker.MESSAGE })[0], is(nullValue()));

			// set an attribute, get its value, then remove it
			String testMessage = createRandomString();
			marker.setAttribute(IMarker.MESSAGE, testMessage);
			assertMarkerHasAttributeValue(marker, IMarker.MESSAGE, testMessage);
			marker.setAttribute(IMarker.MESSAGE, null);
			assertMarkerHasAttributeValue(marker, IMarker.MESSAGE, null);

			// set more attributes, get their values, then remove one
			String[] keys = new String[] { IMarker.LOCATION, IMarker.SEVERITY, IMarker.DONE };
			Object[] values = new Object[3];
			values[0] = createRandomString();
			values[1] = Integer.valueOf(5);
			values[2] = Boolean.FALSE;
			Map<String, Object> originalMap = Map.of(keys[0], values[0], keys[1], values[1], keys[2], values[2]);
			marker.setAttributes(keys, values);
			Object[] found = marker.getAttributes(keys);
			assertThat(resourcePath, found, is(values));
			marker.setAttribute(IMarker.SEVERITY, null);
			values[1] = null;
			found = marker.getAttributes(keys);
			assertThat(resourcePath, found, is(values));
			values[1] = Integer.valueOf(5);
			marker.setAttribute(IMarker.SEVERITY, values[1]);
			assertThat(resourcePath, marker.getAttributes(), is(originalMap));

			// try sending null as args
			assertThrows(resourcePath, RuntimeException.class, () -> marker.getAttribute(null));
			assertThrows(resourcePath, RuntimeException.class, () -> marker.getAttribute(null, "default"));
			assertThrows(resourcePath, RuntimeException.class, () -> marker.getAttribute(null, true));
			assertThrows(resourcePath, RuntimeException.class, () -> marker.getAttribute(null, 5));
			assertThrows(resourcePath, RuntimeException.class, () -> marker.getAttributes(null));
			assertThrows(resourcePath, RuntimeException.class, () -> marker.setAttribute(null, createRandomString()));
			assertThrows(resourcePath, RuntimeException.class,
					() -> marker.setAttributes(null, new String[] { createRandomString() }));
			assertThrows(resourcePath, RuntimeException.class,
					() -> marker.setAttributes(new String[] { IMarker.MESSAGE }, null));

			Map<String, Object> retrievedAttributes = marker.getAttributes();
			retrievedAttributes.put("1", null); // allowed for clients using IMarker.getAttributes()
			retrievedAttributes.put("2", 2);
			marker.setAttributes(retrievedAttributes);
			assertMarkerHasAttributeValue(marker, "1", null);
			assertMarkerHasAttributeValue(marker, "2", 2);
			retrievedAttributes.put(null, 1); // allowed for clients using IMarker.getAttributes()

			Map<String, Object> reretrievedAttributes = marker.getAttributes();
			reretrievedAttributes.put(null, 1); // allowed for clients using IMarker.getAttributes()
			// not allowed for clients to put null key
			assertThrows(resourcePath, RuntimeException.class, () -> marker.setAttributes(reretrievedAttributes));
			assertThat(resourcePath, marker.getAttribute("2"), not(is(nullValue())));
			assertThat(resourcePath, marker.getAttributes(), not(is(nullValue())));
			marker.setAttributes(null);
			assertThat(resourcePath, marker.getAttribute("1"), is(nullValue()));
			assertThat(resourcePath, marker.getAttribute("2"), is(nullValue()));
			assertThat(resourcePath, marker.getAttributes(), is(nullValue()));
		}
	}

}
