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
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_127562;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_EARTH;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_MISSING;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_SIMPLE;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_SNOW;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_WATER;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.getInvalidNatureSets;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.getValidNatureSets;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.CheckMissingNaturesListener;
import org.eclipse.core.internal.resources.File;
import org.eclipse.core.internal.resources.PreferenceInitializer;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.internal.resources.SimpleNature;
import org.junit.Assert;

/**
 * Tests all aspects of project natures.  These tests only
 * exercise API classes and methods.  Note that the nature-related
 * APIs on IWorkspace are tested by IWorkspaceTest.
 */
public class NatureTest extends ResourceTest {
	IProject project;

	/**
	 * Sets the given set of natures for the project.  If success
	 * does not match the "shouldFail" argument, an assertion error
	 * with the given message is thrown.
	 */
	protected void setNatures(String message, IProject project, String[] natures, boolean shouldFail) {
		setNatures(message, project, natures, shouldFail, false);
	}

	/**
	 * Sets the given set of natures for the project.  If success
	 * does not match the "shouldFail" argument, an assertion error
	 * with the given message is thrown.
	 */
	protected void setNatures(String message, IProject project, String[] natures, boolean shouldFail, boolean silent) {
		try {
			IProjectDescription desc = project.getDescription();
			desc.setNatureIds(natures);
			int flags = IResource.KEEP_HISTORY;
			if (silent) {
				flags |= IResource.AVOID_NATURE_CONFIG;
			}
			project.setDescription(desc, flags, getMonitor());
			if (shouldFail) {
				fail(message);
			}
		} catch (CoreException e) {
			if (!shouldFail) {
				fail(message, e);
			}
		}
	}

	@Override
	protected void tearDown() throws Exception {
		project.delete(true, null);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).putInt(ResourcesPlugin.PREF_MISSING_NATURE_MARKER_SEVERITY, PreferenceInitializer.PREF_MISSING_NATURE_MARKER_SEVERITY_DEFAULT);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).flush();
		getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
		super.tearDown();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.project = ResourcesPlugin.getWorkspace().getRoot().getProject(getUniqueString());
	}

	/**
	 * Tests invalid additions to the set of natures for a project.
	 */
	public void testInvalidAdditions() throws CoreException {
		ensureExistsInWorkspace(project, true);
		setNatures("1.0", project, new String[] {NATURE_SIMPLE}, false);

		//Adding a nature that is not available.
		setNatures("2.0", project, new String[] {NATURE_SIMPLE, NATURE_MISSING}, true);
		assertTrue("2.1", project.hasNature(NATURE_SIMPLE));
		assertTrue("2.2", !project.hasNature(NATURE_MISSING));
		assertTrue("2.3", project.isNatureEnabled(NATURE_SIMPLE));
		assertTrue("2.4", !project.isNatureEnabled(NATURE_MISSING));

		//Adding a nature that has a missing prerequisite.
		setNatures("3.0", project, new String[] {NATURE_SIMPLE, NATURE_SNOW}, true);
		assertTrue("3.1", project.hasNature(NATURE_SIMPLE));
		assertTrue("3.2", !project.hasNature(NATURE_SNOW));
		assertTrue("3.3", project.isNatureEnabled(NATURE_SIMPLE));
		assertTrue("3.4", !project.isNatureEnabled(NATURE_SNOW));

		//Adding a nature that creates a duplicated set member.
		setNatures("4.0", project, new String[] {NATURE_EARTH}, false);
		setNatures("4.1", project, new String[] {NATURE_EARTH, NATURE_WATER}, true);
		assertTrue("3.1", project.hasNature(NATURE_EARTH));
		assertTrue("3.2", !project.hasNature(NATURE_WATER));
		assertTrue("3.3", project.isNatureEnabled(NATURE_EARTH));
		assertTrue("3.4", !project.isNatureEnabled(NATURE_WATER));
	}

	/**
	 * Tests invalid removals from the set of natures for a project.
	 */
	public void testInvalidRemovals() throws CoreException {
		ensureExistsInWorkspace(project, true);

		//Removing a nature that still has dependents.
		setNatures("1.0", project, new String[] {NATURE_WATER, NATURE_SNOW}, false);
		setNatures("2.0", project, new String[] {NATURE_SNOW}, true);
		assertTrue("2.1", project.hasNature(NATURE_WATER));
		assertTrue("2.2", project.hasNature(NATURE_SNOW));
		assertTrue("2.3", project.isNatureEnabled(NATURE_WATER));
		assertTrue("2.4", project.isNatureEnabled(NATURE_SNOW));
	}

	public void testNatureLifecyle() throws CoreException {
		ensureExistsInWorkspace(project, true);

		//add simple nature
		setNatures("1.0", project, new String[] {NATURE_SIMPLE}, false);
		SimpleNature instance = SimpleNature.getInstance();
		assertTrue("1.1", instance.wasConfigured);
		assertTrue("1.2", !instance.wasDeconfigured);
		instance.reset();

		//remove simple nature
		setNatures("1.3", project, new String[0], false);
		instance = SimpleNature.getInstance();
		assertTrue("1.4", !instance.wasConfigured);
		assertTrue("1.5", instance.wasDeconfigured);

		//add with AVOID_NATURE_CONFIG
		instance.reset();
		setNatures("2.0", project, new String[] {NATURE_SIMPLE}, false, true);
		instance = SimpleNature.getInstance();
		assertTrue("2.1", !instance.wasConfigured);
		assertTrue("2.2", !instance.wasDeconfigured);
		assertTrue("2.3", project.hasNature(NATURE_SIMPLE));

		//remove with AVOID_NATURE_CONFIG
		instance.reset();
		setNatures("2.3", project, new String[0], false, true);
		instance = SimpleNature.getInstance();
		assertTrue("2.4", !instance.wasConfigured);
		assertTrue("2.5", !instance.wasDeconfigured);
		assertTrue("2.6", !project.hasNature(NATURE_SIMPLE));
	}

	/**
	 * Test simple addition and removal of natures.
	 */
	public void testSimpleNature() throws CoreException {
		ensureExistsInWorkspace(project, true);

		String[][] valid = getValidNatureSets();
		for (int i = 0; i < valid.length; i++) {
			setNatures("valid: " + i, project, valid[i], false);
		}
		//configure a valid nature before starting invalid tests
		String[] currentSet = new String[] {NATURE_SIMPLE};
		setNatures("1.0", project, currentSet, false);

		//now do invalid tests and ensure simple nature is still configured
		String[][] invalid = getInvalidNatureSets();
		for (int i = 0; i < invalid.length; i++) {
			setNatures("invalid: " + i, project, invalid[i], true);
			assertTrue("2.0", project.hasNature(NATURE_SIMPLE));
			assertTrue("2.1", !project.hasNature(NATURE_EARTH));
			assertTrue("2.2", project.isNatureEnabled(NATURE_SIMPLE));
			assertTrue("2.3", !project.isNatureEnabled(NATURE_EARTH));
			assertEquals("2.4", project.getDescription().getNatureIds(), currentSet);
		}
	}

	/**
	 * Test addition of nature that requires the workspace root.
	 * See bugs 127562 and  128709.
	 */
	public void testBug127562Nature() throws CoreException {
		ensureExistsInWorkspace(project, true);
		IWorkspace ws = project.getWorkspace();

		String[][] valid = getValidNatureSets();
		for (int i = 0; i < valid.length; i++) {
			setNatures("valid: " + i, project, valid[i], false);
		}

		// add with AVOID_NATURE_CONFIG
		String[] currentSet = new String[] {NATURE_127562};
		setNatures("1.0", project, currentSet, false, true);

		// configure the nature using a conflicting scheduling rule
		IJobManager manager = Job.getJobManager();
		try {
			manager.beginRule(ws.getRuleFactory().modifyRule(project), null);
			assertThrows(IllegalArgumentException.class, () -> project.getNature(NATURE_127562).configure());
		} finally {
			manager.endRule(ws.getRuleFactory().modifyRule(project));
		}

		// configure the nature using a non-conflicting scheduling rule
		try {
			manager.beginRule(ws.getRoot(), null);
			project.getNature(NATURE_127562).configure();
		} finally {
			manager.endRule(ws.getRoot());
		}
	}

	public void testBug297871() throws Exception {
		ensureExistsInWorkspace(project, true);

		IFileStore descStore = ((File) project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME)).getStore();
		java.io.File desc = descStore.toLocalFile(EFS.NONE, getMonitor());

		java.io.File descTmp = new java.io.File(desc.getPath() + ".tmp");
		copy(desc, descTmp);

		setNatures("valid ", project, new String[] {NATURE_EARTH}, false);

		assertNotNull(project.getNature(NATURE_EARTH));
		assertTrue(project.hasNature(NATURE_EARTH));

		// Make sure enough time has past to bump file's
		// timestamp during the copy
		Thread.sleep(1000);

		copy(descTmp, desc);

		project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		assertNull(project.getNature(NATURE_EARTH));
		assertFalse(project.hasNature(NATURE_EARTH));
	}

	private void copy(java.io.File src, java.io.File dst) throws IOException {
		try (
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dst);
		) {
			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) > 0) {
				out.write(buffer, 0, read);
			}
		}
	}

	/**
	 * Changes project description and parallel checks {@link IProject#isNatureEnabled(String)},
	 * to check if natures value is cached properly.
	 *
	 * See Bug 338055.
	 */
	public void testBug338055() throws Exception {
		final boolean finished[] = new boolean[] {false};
		ensureExistsInWorkspace(project, true);

		new Job("CheckNatureJob") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					if (finished[0] == false) {
						if (project.exists() && project.isOpen()) {
							project.isNatureEnabled(NATURE_SIMPLE);
						}
						schedule();
					}
				} catch (CoreException e) {
					fail("CheckNatureJob failed", e);
				}
				return Status.OK_STATUS;
			}
		}.schedule();

		// Make sure enough time has past to bump file's
		// timestamp during the copy
		Thread.sleep(1000);

		IFileStore descStore = ((File) project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME)).getStore();

		// create a description with many natures, this will make updating description longer
		StringBuilder description = new StringBuilder();
		description.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><projectDescription><name></name><comment></comment><projects></projects><buildSpec></buildSpec><natures>");
		description.append("<nature>" + NATURE_SIMPLE + "</nature>");
		for (int i = 0; i < 100; i++) {
			description.append("<nature>nature" + i + "</nature>");
		}
		description.append("</natures></projectDescription>\n");

		// write the description
		try (OutputStream output = descStore.openOutputStream(EFS.NONE, getMonitor());) {
			output.write(description.toString().getBytes());
		}

		project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		finished[0] = true;

		assertTrue("4.0", project.hasNature(NATURE_SIMPLE));
		assertTrue("5.0", project.isNatureEnabled(NATURE_SIMPLE));
	}

	public void testMissingNatureAddsMarker() throws Exception {
		ensureExistsInWorkspace(project, true);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).putInt(ResourcesPlugin.PREF_MISSING_NATURE_MARKER_SEVERITY, IMarker.SEVERITY_WARNING);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).flush();
		IProjectDescription desc = project.getDescription();
		desc.setNatureIds(new String[] {NATURE_MISSING});
		project.setDescription(desc, IResource.FORCE | IResource.AVOID_NATURE_CONFIG, getMonitor());
		project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		project.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		Job.getJobManager().wakeUp(CheckMissingNaturesListener.MARKER_TYPE);
		Job.getJobManager().join(CheckMissingNaturesListener.MARKER_TYPE, getMonitor());
		IMarker[] markers = project.findMarkers(CheckMissingNaturesListener.MARKER_TYPE, false, IResource.DEPTH_ONE);
		Assert.assertEquals(1, markers.length);
		IMarker marker = markers[0];
		Assert.assertEquals(NATURE_MISSING, marker.getAttribute("natureId"));
		Assert.assertNotEquals(-42, marker.getAttribute(IMarker.CHAR_START, -42));
		Assert.assertNotEquals(-42, marker.getAttribute(IMarker.CHAR_END, -42));
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); InputStream input = ((IFile) marker.getResource()).getContents()) {
			FileUtil.transferStreams(input, bos, "whatever", getMonitor());
			String marked = bos.toString().substring(marker.getAttribute(IMarker.CHAR_START, -42), marker.getAttribute(IMarker.CHAR_END, -42));
			Assert.assertEquals(NATURE_MISSING, marked);
		}
	}

	public void testMissingNatureWithWhitespacesSetChars() throws Exception {
		ensureExistsInWorkspace(project, true);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).putInt(ResourcesPlugin.PREF_MISSING_NATURE_MARKER_SEVERITY, IMarker.SEVERITY_WARNING);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).flush();
		IFile dotProjectFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		dotProjectFile.setContents(new ByteArrayInputStream(("<projectDescription><name>" + project.getName() + "</name><natures><nature> " + NATURE_MISSING + "  </nature></natures></projectDescription>").getBytes()), false, false, getMonitor());
		project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		project.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		Job.getJobManager().wakeUp(CheckMissingNaturesListener.MARKER_TYPE);
		Job.getJobManager().join(CheckMissingNaturesListener.MARKER_TYPE, getMonitor());
		IMarker[] markers = project.findMarkers(CheckMissingNaturesListener.MARKER_TYPE, false, IResource.DEPTH_ONE);
		Assert.assertEquals(1, markers.length);
		IMarker marker = markers[0];
		Assert.assertEquals(NATURE_MISSING, marker.getAttribute("natureId"));
		Assert.assertNotEquals(-42, marker.getAttribute(IMarker.CHAR_START, -42));
		Assert.assertNotEquals(-42, marker.getAttribute(IMarker.CHAR_END, -42));
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); InputStream input = ((IFile) marker.getResource()).getContents()) {
			FileUtil.transferStreams(input, bos, "whatever", getMonitor());
			String marked = bos.toString().substring(marker.getAttribute(IMarker.CHAR_START, -42),
					marker.getAttribute(IMarker.CHAR_END, -42));
			Assert.assertEquals(NATURE_MISSING, marked);
		}
	}

	public void testKnownNatureDoesntAddMarker() throws Exception {
		ensureExistsInWorkspace(project, true);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).putInt(ResourcesPlugin.PREF_MISSING_NATURE_MARKER_SEVERITY, IMarker.SEVERITY_WARNING);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).flush();
		IProjectDescription desc = project.getDescription();
		desc.setNatureIds(new String[] {NATURE_SIMPLE});
		project.setDescription(desc, getMonitor());
		project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		project.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		Job.getJobManager().wakeUp(CheckMissingNaturesListener.MARKER_TYPE);
		Job.getJobManager().join(CheckMissingNaturesListener.MARKER_TYPE, getMonitor());
		Assert.assertEquals(0, project.findMarkers(CheckMissingNaturesListener.MARKER_TYPE, false, IResource.DEPTH_ONE).length);
	}

	public void testListenToPreferenceChange() throws Exception {
		testMissingNatureAddsMarker();
		// to INFO
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).putInt(ResourcesPlugin.PREF_MISSING_NATURE_MARKER_SEVERITY, IMarker.SEVERITY_INFO);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).flush();
		Job.getJobManager().wakeUp(CheckMissingNaturesListener.MARKER_TYPE);
		Job.getJobManager().join(CheckMissingNaturesListener.MARKER_TYPE, getMonitor());
		IMarker[] markers = project.findMarkers(CheckMissingNaturesListener.MARKER_TYPE, false, IResource.DEPTH_ONE);
		Assert.assertEquals(1, markers.length);
		Assert.assertEquals(IMarker.SEVERITY_INFO, markers[0].getAttribute(IMarker.SEVERITY, -42));
		// to IGNORE
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).putInt(ResourcesPlugin.PREF_MISSING_NATURE_MARKER_SEVERITY, -1);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).flush();
		Job.getJobManager().wakeUp(CheckMissingNaturesListener.MARKER_TYPE);
		Job.getJobManager().join(CheckMissingNaturesListener.MARKER_TYPE, getMonitor());
		markers = project.findMarkers(CheckMissingNaturesListener.MARKER_TYPE, false, IResource.DEPTH_ONE);
		Assert.assertEquals(0, markers.length);
		// to ERROR
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).putInt(ResourcesPlugin.PREF_MISSING_NATURE_MARKER_SEVERITY, IMarker.SEVERITY_ERROR);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).flush();
		Job.getJobManager().wakeUp(CheckMissingNaturesListener.MARKER_TYPE);
		Job.getJobManager().join(CheckMissingNaturesListener.MARKER_TYPE, getMonitor());
		markers = project.findMarkers(CheckMissingNaturesListener.MARKER_TYPE, false, IResource.DEPTH_ONE);
		Assert.assertEquals(1, markers.length);
		Assert.assertEquals(IMarker.SEVERITY_ERROR, markers[0].getAttribute(IMarker.SEVERITY, -42));
	}
}
