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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
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
import org.eclipse.core.tests.resources.ResourceTestUtil;
import org.eclipse.core.tests.resources.TestUtil;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the IRefreshMonitor interface
 */
@ExtendWith(WorkspaceResetExtension.class)
public class RefreshProviderTest {

	private boolean originalRefreshSetting;

	@BeforeEach
	public void setUp() {
		//turn on autorefresh
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		originalRefreshSetting = prefs.getBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, false);
		prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, true);
		ResourceTestUtil.waitForRefresh();
		TestRefreshProvider.reset();
	}

	@AfterEach
	public void tearDown() {
		//turn off autorefresh
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, originalRefreshSetting);
		ResourceTestUtil.waitForRefresh();
		TestRefreshProvider.reset();
	}

	/**
	 * Test to ensure that a refresh provider is given the correct events when a linked
	 * file is created and deleted.
	 */
	@Test
	public void testLinkedFile(@TempDir Path tempDirectory) throws Exception {
		IPath location = IPath.fromPath(tempDirectory).append("test");
		String name = "testUnmonitorLinkedResource";
		IProject project = getWorkspace().getRoot().getProject(name);
		createInWorkspace(project);
		joinAutoRefreshJobs();
		IFile link = project.getFile("Link");
		// ensure we currently have just the project being monitored
		TestRefreshProvider provider = TestRefreshProvider.getInstance();
		assertThat(provider.getMonitoredResources()).hasSize(1);
		link.createLink(location, IResource.ALLOW_MISSING_LOCAL, createTestMonitor());
		joinAutoRefreshJobs();
		assertThat(provider.getMonitoredResources()).hasSize(2);
		link.delete(IResource.FORCE, createTestMonitor());
		joinAutoRefreshJobs();
		assertThat(provider.getMonitoredResources()).hasSize(1);
		removeFromWorkspace(project);
		joinAutoRefreshJobs();
		assertThat(provider.getMonitoredResources()).isEmpty();
		// check provider for other errors
		AssertionError[] failures = provider.getFailures();
		assertThat(failures).isEmpty();
	}

	/**
	 * Test to ensure that a refresh provider is given the correct events when a project
	 * is closed or opened.
	 */
	@Test
	public void testProjectCloseOpen() throws Exception {
		String name = "testProjectCloseOpen";
		IProject project = getWorkspace().getRoot().getProject(name);
		createInWorkspace(project);
		joinAutoRefreshJobs();
		// ensure we currently have just the project being monitored
		TestRefreshProvider provider = TestRefreshProvider.getInstance();
		assertThat(provider.getMonitoredResources()).hasSize(1);
		project.close(createTestMonitor());
		joinAutoRefreshJobs();
		assertThat(provider.getMonitoredResources()).isEmpty();
		project.open(createTestMonitor());
		joinAutoRefreshJobs();
		assertThat(provider.getMonitoredResources()).hasSize(1);
		removeFromWorkspace(project);
		joinAutoRefreshJobs();
		assertThat(provider.getMonitoredResources()).isEmpty();
		// check provider for other errors
		AssertionError[] failures = provider.getFailures();
		assertThat(failures).isEmpty();
	}

	/**
	 * Test to ensure that a refresh provider is given the correct events when a project
	 * is closed or opened.
	 */
	@Test
	public void testProjectCreateDelete(TestInfo testInfo) throws Exception {
		String name = "testProjectCreateDelete";
		final int maxRuns = 1000;
		int i = 0;
		Map<Integer, Throwable> fails = new HashMap<>();
		for (; i < maxRuns; i++) {
			if (i % 50 == 0) {
				TestUtil.waitForJobs(testInfo.getDisplayName(), 5, 100);
			}
			try {
				assertTrue(createProject(name).isAccessible());
				assertFalse(deleteProject(name).exists());
			} catch (CoreException e) {
				fails.put(i, e);
			}
		}
		assertThat(fails).isEmpty();
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
