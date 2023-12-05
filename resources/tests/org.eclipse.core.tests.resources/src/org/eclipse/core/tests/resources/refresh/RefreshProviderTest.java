/*******************************************************************************
 *  Copyright (c) 2004, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.refresh;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.emptyArray;

import java.util.HashMap;
import java.util.Map;
import junit.framework.AssertionFailedError;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.resources.ResourceTest;
import org.eclipse.core.tests.resources.TestUtil;

/**
 * Tests the IRefreshMonitor interface
 */
public class RefreshProviderTest extends ResourceTest {

	private boolean originalRefreshSetting;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		TestRefreshProvider.reset();
		//turn on autorefresh
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		originalRefreshSetting = prefs.getBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, false);
		prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, true);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		//turn off autorefresh
		TestRefreshProvider.reset();
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, originalRefreshSetting);
	}

	/**
	 * Test to ensure that a refresh provider is given the correct events when a linked
	 * file is created and deleted.
	 */
	public void testLinkedFile() throws Exception {
		IPath location = getRandomLocation();
		deleteOnTearDown(location);
		String name = "testUnmonitorLinkedResource";
		IProject project = getWorkspace().getRoot().getProject(name);
		ensureExistsInWorkspace(project, true);
		joinAutoRefreshJobs();
		IFile link = project.getFile("Link");
		// ensure we currently have just the project being monitored
		TestRefreshProvider provider = TestRefreshProvider.getInstance();
		assertEquals("1.0", 1, provider.getMonitoredResources().length);
		link.createLink(location, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		joinAutoRefreshJobs();
		assertEquals("1.1", 2, provider.getMonitoredResources().length);
		link.delete(IResource.FORCE, getMonitor());
		joinAutoRefreshJobs();
		assertEquals("1.2", 1, provider.getMonitoredResources().length);
		ensureDoesNotExistInWorkspace(project);
		joinAutoRefreshJobs();
		assertEquals("1.3", 0, provider.getMonitoredResources().length);
		// check provider for other errors
		AssertionFailedError[] failures = provider.getFailures();
		assertThat(failures, emptyArray());
	}

	/**
	 * Test to ensure that a refresh provider is given the correct events when a project
	 * is closed or opened.
	 */
	public void testProjectCloseOpen() throws Exception {
		String name = "testProjectCloseOpen";
		IProject project = getWorkspace().getRoot().getProject(name);
		ensureExistsInWorkspace(project, true);
		joinAutoRefreshJobs();
		// ensure we currently have just the project being monitored
		TestRefreshProvider provider = TestRefreshProvider.getInstance();
		assertEquals("1.0", 1, provider.getMonitoredResources().length);
		project.close(getMonitor());
		joinAutoRefreshJobs();
		assertEquals("1.1", 0, provider.getMonitoredResources().length);
		project.open(getMonitor());
		joinAutoRefreshJobs();
		assertEquals("1.2", 1, provider.getMonitoredResources().length);
		ensureDoesNotExistInWorkspace(project);
		joinAutoRefreshJobs();
		assertEquals("1.3", 0, provider.getMonitoredResources().length);
		// check provider for other errors
		AssertionFailedError[] failures = provider.getFailures();
		assertThat(failures, emptyArray());
	}

	/**
	 * Test to ensure that a refresh provider is given the correct events when a project
	 * is closed or opened.
	 */
	public void testProjectCreateDelete() throws Exception {
		String name = "testProjectCreateDelete";
		final int maxRuns = 1000;
		int i = 0;
		Map<Integer, Throwable> fails = new HashMap<>();
		for (; i < maxRuns; i++) {
			if (i % 50 == 0) {
				TestUtil.waitForJobs(getName(), 5, 100);
			}
			try {
				assertTrue(createProject(name).isAccessible());
				assertFalse(deleteProject(name).exists());
			} catch (CoreException e) {
				fails.put(i, e);
			}
		}
		assertThat(fails, anEmptyMap());
	}

	private IProject createProject(String name) throws Exception {
		IProject pro = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		if (pro.exists()) {
			pro.delete(true, true, null);
		}
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(name);
		if (!project.exists()) {
			project.create(null);
		} else {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		}
		if (!project.isOpen()) {
			project.open(null);
		}
		return project;
	}

	private static IProject deleteProject(String name) throws Exception {
		IProject pro = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		if (pro.exists()) {
			pro.delete(true, true, null);
		}
		return pro;
	}

	private void joinAutoRefreshJobs() throws InterruptedException {
		// We must join on the auto-refresh family because the workspace changes done in the
		// tests above may be batched and broadcasted by the RefreshJob, not the main thread.
		// There is then a race condition between the main thread, the refresh job and the job
		// scheduled by MonitorManager.monitorAsync. Thus, we must join on both the RefreshJob
		// and the job scheduled by MonitorManager.monitorAsync. For simplicity, the job
		// scheduled by MonitorManager.monitorAsync has been set to belong to the same family
		// as the RefreshJob.
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, null);
	}
}
