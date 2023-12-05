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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_127562;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_EARTH;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_MISSING;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_SIMPLE;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_SNOW;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_WATER;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.getInvalidNatureSets;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.getValidNatureSets;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;
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
import org.junit.function.ThrowingRunnable;

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
	private void setNatures(IProject project, String[] natures, boolean shouldFail) throws Throwable {
		setNatures(project, natures, shouldFail, false);
	}

	/**
	 * Sets the given set of natures for the project.  If success
	 * does not match the "shouldFail" argument, an assertion error
	 * with the given message is thrown.
	 */
	private void setNatures(IProject project, String[] natures, boolean shouldFail, boolean silent)
			throws Throwable {
		IProjectDescription desc = project.getDescription();
		desc.setNatureIds(natures);
		int flags;
		if (silent) {
			flags = IResource.KEEP_HISTORY | IResource.AVOID_NATURE_CONFIG;
		} else {
			flags = IResource.KEEP_HISTORY;
		}
		ThrowingRunnable descriptionSetter = () -> project.setDescription(desc, flags, getMonitor());
		if (shouldFail) {
			assertThrows(CoreException.class, descriptionSetter);
		} else {
			descriptionSetter.run();
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

	private void assertHasEnabledNature(String nature) throws CoreException {
		assertThat("project '" + project + "' is expected to have nature: " + nature,
				project.hasNature(nature));
		assertThat("project '" + project + "' is expected to have nature enabled: " + nature,
				project.isNatureEnabled(nature));
	}

	private void assertDoesNotHaveNature(String nature) throws CoreException {
		assertThat("project '" + project + "' is not expected to have nature: " + nature,
				!project.hasNature(nature));
		assertThat("project '" + project + "' is not expected to have nature enabled: " + nature,
				!project.isNatureEnabled(nature));
	}

	/**
	 * Tests invalid additions to the set of natures for a project.
	 */
	public void testInvalidAdditions() throws Throwable {
		ensureExistsInWorkspace(project, true);
		setNatures(project, new String[] { NATURE_SIMPLE }, false);

		//Adding a nature that is not available.
		setNatures(project, new String[] { NATURE_SIMPLE, NATURE_MISSING }, true);
		assertHasEnabledNature(NATURE_SIMPLE);
		assertDoesNotHaveNature(NATURE_MISSING);

		//Adding a nature that has a missing prerequisite.
		setNatures(project, new String[] { NATURE_SIMPLE, NATURE_SNOW }, true);
		assertHasEnabledNature(NATURE_SIMPLE);
		assertDoesNotHaveNature(NATURE_SNOW);

		//Adding a nature that creates a duplicated set member.
		setNatures(project, new String[] { NATURE_EARTH }, false);
		setNatures(project, new String[] { NATURE_EARTH, NATURE_WATER }, true);
		assertHasEnabledNature(NATURE_EARTH);
		assertDoesNotHaveNature(NATURE_WATER);
	}

	/**
	 * Tests invalid removals from the set of natures for a project.
	 */
	public void testInvalidRemovals() throws Throwable {
		ensureExistsInWorkspace(project, true);

		//Removing a nature that still has dependents.
		setNatures(project, new String[] { NATURE_WATER, NATURE_SNOW }, false);
		setNatures(project, new String[] { NATURE_SNOW }, true);
		assertHasEnabledNature(NATURE_WATER);
		assertHasEnabledNature(NATURE_SNOW);
	}

	public void testNatureLifecyle() throws Throwable {
		ensureExistsInWorkspace(project, true);

		//add simple nature
		setNatures(project, new String[] { NATURE_SIMPLE }, false);
		SimpleNature instance = SimpleNature.getInstance();
		assertThat("Simple nature has not been configured", instance.wasConfigured);
		assertThat("Simple nature has been deconfigured", !instance.wasDeconfigured);
		instance.reset();

		//remove simple nature
		setNatures(project, new String[0], false);
		instance = SimpleNature.getInstance();
		assertThat("Simple nature has been configured", !instance.wasConfigured);
		assertThat("Simple nature has not been deconfigured", instance.wasDeconfigured);

		//add with AVOID_NATURE_CONFIG
		instance.reset();
		setNatures(project, new String[] { NATURE_SIMPLE }, false, true);
		instance = SimpleNature.getInstance();
		assertThat("Simple nature has been configured", !instance.wasConfigured);
		assertThat("Simple nature has been deconfigured", !instance.wasDeconfigured);
		assertHasEnabledNature(NATURE_SIMPLE);

		//remove with AVOID_NATURE_CONFIG
		instance.reset();
		setNatures(project, new String[0], false, true);
		instance = SimpleNature.getInstance();
		assertThat("Simple nature has been configured", !instance.wasConfigured);
		assertThat("Simple nature has been deconfigured", !instance.wasDeconfigured);
		assertDoesNotHaveNature(NATURE_SIMPLE);
	}

	/**
	 * Test simple addition and removal of natures.
	 */
	public void testSimpleNature() throws Throwable {
		ensureExistsInWorkspace(project, true);

		String[][] valid = getValidNatureSets();
		for (String[] element : valid) {
			setNatures(project, element, false);
		}
		//configure a valid nature before starting invalid tests
		String[] currentSet = new String[] {NATURE_SIMPLE};
		setNatures(project, currentSet, false);

		//now do invalid tests and ensure simple nature is still configured
		String[][] invalid = getInvalidNatureSets();
		for (String[] element : invalid) {
			setNatures(project, element, true);
			assertHasEnabledNature(NATURE_SIMPLE);
			assertDoesNotHaveNature(NATURE_WATER);
			assertThat(currentSet, is(project.getDescription().getNatureIds()));
		}
	}

	/**
	 * Test addition of nature that requires the workspace root.
	 * See bugs 127562 and  128709.
	 */
	public void testBug127562Nature() throws Throwable {
		ensureExistsInWorkspace(project, true);
		IWorkspace ws = project.getWorkspace();

		String[][] valid = getValidNatureSets();
		for (String[] element : valid) {
			setNatures(project, element, false);
		}

		// add with AVOID_NATURE_CONFIG
		String[] currentSet = new String[] {NATURE_127562};
		setNatures(project, currentSet, false, true);

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

	public void testBug297871() throws Throwable {
		ensureExistsInWorkspace(project, true);

		IFileStore descStore = ((File) project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME)).getStore();
		java.io.File desc = descStore.toLocalFile(EFS.NONE, getMonitor());

		java.io.File descTmp = new java.io.File(desc.getPath() + ".tmp");
		copy(desc, descTmp);

		setNatures(project, new String[] { NATURE_EARTH }, false);

		assertHasEnabledNature(NATURE_EARTH);
		assertThat(project.getNature(NATURE_EARTH), not(nullValue()));

		// Make sure enough time has past to bump file's
		// timestamp during the copy
		Thread.sleep(1000);

		copy(descTmp, desc);

		project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		assertDoesNotHaveNature(NATURE_EARTH);
		assertThat(project.getNature(NATURE_EARTH), nullValue());
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
		final boolean finished[] = new boolean[1];
		ensureExistsInWorkspace(project, true);

		AtomicReference<CoreException> failureInJob = new AtomicReference<>();
		Job simulateNatureAccessJob = new Job("CheckNatureJob") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					if (!finished[0]) {
						if (project.exists() && project.isOpen()) {
							project.isNatureEnabled(NATURE_SIMPLE);
						}
						schedule();
					}
				} catch (CoreException e) {
					failureInJob.set(e);
				}
				return Status.OK_STATUS;
			}
		};
		simulateNatureAccessJob.schedule();

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
		simulateNatureAccessJob.join();
		assertHasEnabledNature(NATURE_SIMPLE);
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
		assertThat(markers, arrayWithSize(1));
		IMarker marker = markers[0];
		assertThat(marker.getAttribute("natureId"), is(NATURE_MISSING));
		assertThat(marker.getAttribute(IMarker.CHAR_START, -42), not(-42));
		assertThat(marker.getAttribute(IMarker.CHAR_END, -42), not(-42));
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); InputStream input = ((IFile) marker.getResource()).getContents()) {
			FileUtil.transferStreams(input, bos, "whatever", getMonitor());
			String marked = bos.toString().substring(marker.getAttribute(IMarker.CHAR_START, -42), marker.getAttribute(IMarker.CHAR_END, -42));
			assertThat(marked, is(NATURE_MISSING));
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
		assertThat(markers, arrayWithSize(1));
		IMarker marker = markers[0];
		assertThat(marker.getAttribute("natureId"), is(NATURE_MISSING));
		assertThat(marker.getAttribute(IMarker.CHAR_START, -42), not(-42));
		assertThat(marker.getAttribute(IMarker.CHAR_END, -42), not(-42));
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); InputStream input = ((IFile) marker.getResource()).getContents()) {
			FileUtil.transferStreams(input, bos, "whatever", getMonitor());
			String marked = bos.toString().substring(marker.getAttribute(IMarker.CHAR_START, -42),
					marker.getAttribute(IMarker.CHAR_END, -42));
			assertThat(marked, is(NATURE_MISSING));
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
		assertThat(project.findMarkers(CheckMissingNaturesListener.MARKER_TYPE, false, IResource.DEPTH_ONE),
				emptyArray());
	}

	public void testListenToPreferenceChange() throws Exception {
		testMissingNatureAddsMarker();
		// to INFO
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).putInt(ResourcesPlugin.PREF_MISSING_NATURE_MARKER_SEVERITY, IMarker.SEVERITY_INFO);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).flush();
		Job.getJobManager().wakeUp(CheckMissingNaturesListener.MARKER_TYPE);
		Job.getJobManager().join(CheckMissingNaturesListener.MARKER_TYPE, getMonitor());
		IMarker[] markers = project.findMarkers(CheckMissingNaturesListener.MARKER_TYPE, false, IResource.DEPTH_ONE);
		assertThat(markers, arrayWithSize(1));
		assertThat(markers[0].getAttribute(IMarker.SEVERITY, -42), is(IMarker.SEVERITY_INFO));
		// to IGNORE
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).putInt(ResourcesPlugin.PREF_MISSING_NATURE_MARKER_SEVERITY, -1);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).flush();
		Job.getJobManager().wakeUp(CheckMissingNaturesListener.MARKER_TYPE);
		Job.getJobManager().join(CheckMissingNaturesListener.MARKER_TYPE, getMonitor());
		markers = project.findMarkers(CheckMissingNaturesListener.MARKER_TYPE, false, IResource.DEPTH_ONE);
		assertThat(markers, arrayWithSize(0));
		// to ERROR
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).putInt(ResourcesPlugin.PREF_MISSING_NATURE_MARKER_SEVERITY, IMarker.SEVERITY_ERROR);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).flush();
		Job.getJobManager().wakeUp(CheckMissingNaturesListener.MARKER_TYPE);
		Job.getJobManager().join(CheckMissingNaturesListener.MARKER_TYPE, getMonitor());
		markers = project.findMarkers(CheckMissingNaturesListener.MARKER_TYPE, false, IResource.DEPTH_ONE);
		assertThat(markers, arrayWithSize(1));
		assertThat(markers[0].getAttribute(IMarker.SEVERITY, -42), is(IMarker.SEVERITY_ERROR));
	}
}
