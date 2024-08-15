/*******************************************************************************
 *  Copyright (c) 2022 Andrey Loskutov <loskutov@gmx.de> and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.internal.refresh.RefreshJob.BASE_REFRESH_DEPTH;
import static org.eclipse.core.internal.refresh.RefreshJob.DEPTH_INCREASE_STEP;
import static org.eclipse.core.internal.refresh.RefreshJob.FAST_REFRESH_THRESHOLD;
import static org.eclipse.core.internal.refresh.RefreshJob.MAX_RECURSION;
import static org.eclipse.core.internal.refresh.RefreshJob.SLOW_REFRESH_THRESHOLD;
import static org.eclipse.core.internal.refresh.RefreshJob.UPDATE_DELAY;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.internal.refresh.RefreshJob;
import org.eclipse.core.internal.refresh.RefreshManager;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.resources.TestUtil;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for RefreshJob
 */
@ExtendWith(WorkspaceResetExtension.class)
public class RefreshJobTest {

	private static final String REFRESH_JOB_FIELD_NAME = "refreshJob";
	private boolean defaultRefresh;
	private boolean shouldResetDefault;

	int fastRefreshThreshold = FAST_REFRESH_THRESHOLD;
	int slowRefreshThreshold = SLOW_REFRESH_THRESHOLD;
	int baseRefreshDepth = BASE_REFRESH_DEPTH;
	int depthIncreaseStep = DEPTH_INCREASE_STEP;
	int updateDelay = UPDATE_DELAY;
	int maxRecursionDeep = MAX_RECURSION;

	private RefreshJob originalJob;

	@BeforeEach
	public void setUp() {
		IEclipsePreferences prefs = getPrefs();
		defaultRefresh = prefs.getBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, false);
		if (defaultRefresh) {
			prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, false);
			shouldResetDefault = true;
		}
		TestUtil.waitForJobs("setup", 100, 5000);
		// we don't want to wait extra time
		updateDelay = 0;
	}

	@AfterEach
	public void tearDown() throws Exception {
		restoreRefreshJob();
		if (shouldResetDefault) {
			getPrefs().putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, defaultRefresh);
		}
	}

	private IEclipsePreferences getPrefs() {
		return InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
	}

	/**
	 * Test to ensure that there is no endless loop on refresh
	 */
	@Test
	public void testBug578487_refreshLoop() throws Exception {
		String name = "testBug578487_refreshLoop";
		int minDepth = 0;
		int maxDepth = maxRecursionDeep;

		int filesCount = 0;
		// 9 dirs & 2 depth & 2 depthIncreaseStep == hang
		int directoriesCount = 9;
		int createDepth = 2;

		depthIncreaseStep = 2;
		fastRefreshThreshold = Integer.MAX_VALUE / 2;
		slowRefreshThreshold = Integer.MAX_VALUE;
		baseRefreshDepth = BASE_REFRESH_DEPTH;
		runtest(name, minDepth, maxDepth, directoriesCount, filesCount, createDepth);
	}

	/**
	 * Just a trivial test that few directories can be refreshed with default
	 * settings and default max depth of 2
	 */
	@Test
	public void testBasicRefresh() throws Exception {
		String name = "testBasicRefresh";
		int minDepth = 0;
		int maxDepth = 2;

		int directoriesCount = 3;
		int filesCount = 1;
		int createDepth = 2;

		runtest(name, minDepth, maxDepth, directoriesCount, filesCount, createDepth);
	}

	/**
	 * Test that few directories can be refreshed with max depth of 16 (simulating a
	 * very fast file system)
	 */
	@Test
	public void testFastRefresh() throws Exception {
		String name = "testFastRefresh";
		int minDepth = 0;
		int maxDepth = 16;

		int directoriesCount = 3;
		int filesCount = 1;
		int createDepth = 2;

		depthIncreaseStep = 2;
		fastRefreshThreshold = Integer.MAX_VALUE / 2;
		slowRefreshThreshold = Integer.MAX_VALUE;
		baseRefreshDepth = BASE_REFRESH_DEPTH;

		runtest(name, minDepth, maxDepth, directoriesCount, filesCount, createDepth);
	}

	/**
	 * Test that lot of directories can be refreshed with max depth of 8
	 */
	@Test
	@Disabled("test is disabled because it needs 400 seconds on fast SSD on Linux")
	public void testSmallRecursionRefresh() throws Exception {
		String name = "testSmallRecursionRefresh";
		maxRecursionDeep = 8;
		int minDepth = 0;
		int maxDepth = maxRecursionDeep;

		int directoriesCount = 6;
		int filesCount = 0;
		int createDepth = 600;

		depthIncreaseStep = 1;
		fastRefreshThreshold = Integer.MAX_VALUE / 2;
		slowRefreshThreshold = Integer.MAX_VALUE;
		baseRefreshDepth = BASE_REFRESH_DEPTH;

		runtest(name, minDepth, maxDepth, directoriesCount, filesCount, createDepth);
	}

	/**
	 * Test that lot of directories can be refreshed with max possible depth
	 */
	@Test
	@Disabled("test is disabled because it needs 250 seconds on fast SSD on Linux")
	public void testBigRecursionDeepRefresh() throws Exception {
		String name = "testBigRecursionDeepRefresh";
		maxRecursionDeep = MAX_RECURSION;// 2 << 29; // 1073741824
		int minDepth = 0;
		int maxDepth = maxRecursionDeep;

		int directoriesCount = 6;
		int filesCount = 0;
		int createDepth = 600;

		depthIncreaseStep = 1;
		fastRefreshThreshold = Integer.MAX_VALUE / 2;
		slowRefreshThreshold = Integer.MAX_VALUE;
		baseRefreshDepth = BASE_REFRESH_DEPTH;

		runtest(name, minDepth, maxDepth, directoriesCount, filesCount, createDepth);
	}

	/**
	 * Test that few directories can be refreshed with max depth of 1 (simulating a
	 * very slow file system)
	 */
	@Test
	public void testSlowRefresh() throws Exception {
		String name = "testSlowRefresh";
		int minDepth = 0;
		int maxDepth = 1;

		int directoriesCount = 3;
		int filesCount = 1;
		int createDepth = 2;

		depthIncreaseStep = 1;
		fastRefreshThreshold = Integer.MIN_VALUE;
		slowRefreshThreshold = Integer.MIN_VALUE;
		baseRefreshDepth = BASE_REFRESH_DEPTH;

		runtest(name, minDepth, maxDepth, directoriesCount, filesCount, createDepth);
	}

	/**
	 * RefreshJob should use right rule to refresh resources, so it should wait with
	 * refresh if
	 */
	@Test
	public void testProjectRule(TestInfo testInfo) throws Exception {
		TestRefreshJob refreshJob = createAndReplaceDefaultJob();
		IProject project = createProject(testInfo.getDisplayName());
		try {
			IFolder parent = project.getFolder("parent");
			parent.create(true, true, null);
			IFolder child = parent.getFolder("child");
			child.create(true, true, null);
			Set<IResource> expected = new LinkedHashSet<>();
			expected.add(project.getFolder(".settings"));
			expected.add(parent);
			expected.add(child);
			expected.add(project);
			boolean releaseRule = true;
			try {
				Job.getJobManager().beginRule(project, null);
				refreshJob.refresh(project);
				Thread.sleep(1000);
				assertTrue(refreshJob.refreshStarted, "Refresh was not started");
				assertFalse(refreshJob.refreshDone, "Refresh should wait on rule");
				assertEquals(Collections.EMPTY_SET, refreshJob.visitedResources, "Should not visit anything yet");
				Job.getJobManager().endRule(project);
				releaseRule = false;
				TestUtil.waitForJobs(testInfo.getDisplayName(), 100, 1000);
				assertTrue(refreshJob.refreshStarted, "Refresh was not started");
				assertTrue(refreshJob.refreshDone, "Refresh was not finished");
				assertEquals(expected, refreshJob.visitedResources, "Missing refresh");
			} finally {
				if (releaseRule) {
					Job.getJobManager().endRule(project);
				}
			}
		} finally {
			deleteProject(testInfo.getDisplayName());
		}
	}

	// Disabled for now, is unstable
	@Test
	@Disabled("disabled for now, is unstable")
	public void testUnrelatedRule(TestInfo testInfo) throws Exception {
		TestRefreshJob refreshJob = createAndReplaceDefaultJob();
		IProject project = createProject(testInfo.getDisplayName());
		try {
			IFolder parent = project.getFolder("parent");
			parent.create(true, true, null);
			IFolder child = parent.getFolder("child");
			child.create(true, true, null);

			Set<IResource> expected = new LinkedHashSet<>();
			IFolder rule = project.getFolder(".settings");
			expected.add(child);
			boolean releaseRule = true;
			try {
				Job.getJobManager().beginRule(rule, null);
				refreshJob.refresh(child);
				TestUtil.waitForJobs(testInfo.getDisplayName(), 10, 60_000, ResourcesPlugin.FAMILY_AUTO_REFRESH);
				assertTrue(refreshJob.refreshStarted, "Refresh was not started");
				assertTrue(refreshJob.refreshDone, "Refresh was not finished");
				Job.getJobManager().endRule(rule);
				releaseRule = false;
				assertEquals(expected, refreshJob.visitedResources, "Missing refresh");
			} finally {
				if (releaseRule) {
					Job.getJobManager().endRule(rule);
				}
			}
		} finally {
			deleteProject(testInfo.getDisplayName());
		}
	}

	private void runtest(String name, int minDepth, int maxDepth, int directoriesCount, int filesCount,
			int createDepth) throws Exception, CoreException {
		try {
			IProject project = createProject(name);
			IPath projectRoot = project.getLocation();
			project.close(null);

			AtomicInteger result = new AtomicInteger(0);
			createDirectoriesViaFileIo(projectRoot.toFile().toPath(), directoriesCount, filesCount, createDepth,
					result);
			assertTrue(result.get() > 0, "Expect to generate some files");
			System.out.println("\nTest " + name + " generated " + result + " files");

			project.open(null);
			TestRefreshJob refreshJob = createAndReplaceDefaultJob();
			refreshJob.refresh(project);
			refreshJob.join();
			assertAllResourcesRefreshed(project, refreshJob);
			assertDepth(refreshJob, minDepth, maxDepth);
		} finally {
			deleteProject(name);
		}
	}

	private void assertDepth(TestRefreshJob refreshJob, int minDepth, int maxDepth) {
		assertEquals(minDepth, refreshJob.minDepth, "Unexpected min depth");
		assertEquals(maxDepth, refreshJob.maxDepth, "Unexpected max depth");
	}

	private void assertAllResourcesRefreshed(IProject project, TestRefreshJob refreshJob) throws Exception {
		Set<IResource> resources = refreshJob.visitedResources;
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		Set<IResource> missing = new LinkedHashSet<>();
		Set<IResource> visited = new LinkedHashSet<>();
		project.accept(resource -> {
			if (resource.getType() == IResource.FILE) {
				return true;
			}
			visited.add(resource);
			if (!resources.contains(resource)) {
				missing.add(resource);
			}
			return true;
		});
		assertThat(missing).as("resources not refreshed").isEmpty();
		assertThat(visited).as("visited projects").isNotEmpty();
		assertThat(resources).as("refreshed resources").isNotEmpty();
	}

	private TestRefreshJob createAndReplaceDefaultJob() throws Exception {
		TestRefreshJob job = new TestRefreshJob(fastRefreshThreshold, slowRefreshThreshold, baseRefreshDepth,
				depthIncreaseStep, updateDelay, maxRecursionDeep);

		RefreshManager refreshManager = ((Workspace) getWorkspace()).getRefreshManager();
		Field field = RefreshManager.class.getDeclaredField(REFRESH_JOB_FIELD_NAME);
		field.setAccessible(true);
		originalJob = (RefreshJob) field.get(refreshManager);
		field.set(refreshManager, job);
		return job;
	}

	private void restoreRefreshJob() throws Exception {
		RefreshManager refreshManager = ((Workspace) getWorkspace()).getRefreshManager();
		Field field = RefreshManager.class.getDeclaredField(REFRESH_JOB_FIELD_NAME);
		field.setAccessible(true);
		field.set(refreshManager, originalJob);
	}

	private void createDirectoriesViaFileIo(Path root, int directoriesCount, int filesCount, int createDepth,
			AtomicInteger result)
			throws Exception {
		if (createDepth <= 0) {
			return;
		}
		if (directoriesCount <= 0) {
			directoriesCount = 1;
		}
		List<Path> dirs = new ArrayList<>();
		for (int i = 0; i < directoriesCount; i++) {
			Path dir = Files.createDirectory(root.resolve("dir_" + i));
			result.incrementAndGet();
			dirs.add(dir);
			for (int j = 0; j < filesCount; j++) {
				Files.createFile(dir.resolve("file_" + j));
				result.incrementAndGet();
			}
		}
		createDepth--;
		directoriesCount--;
		filesCount--;
		for (Path dir : dirs) {
			createDirectoriesViaFileIo(dir, directoriesCount, filesCount, createDepth, result);
		}
	}

	private IProject createProject(String name) throws Exception {
		IWorkspaceRoot root = getWorkspaceRoot();
		assertFalse(deleteProject(name).isAccessible());
		IProject project = root.getProject(name);
		project.create(null);
		project.open(null);
		return project;
	}


	private static IWorkspaceRoot getWorkspaceRoot() {
		return getWorkspace().getRoot();
	}

	private static IProject deleteProject(String name) throws Exception {
		IProject pro = getWorkspaceRoot().getProject(name);
		if (pro.exists()) {
			pro.delete(true, true, null);
		}
		return pro;
	}

	class TestRefreshJob extends RefreshJob {

		int maxDepth;
		int minDepth;
		Set<IResource> visitedResources = new LinkedHashSet<>();
		volatile boolean refreshStarted;
		volatile boolean refreshDone;

		protected TestRefreshJob(int fastRefreshThreshold, int slowRefreshThreshold, int baseRefreshDepth,
				int depthIncreaseStep, int updateDelay, int maxRecursionDeep) {
			super(fastRefreshThreshold, slowRefreshThreshold, baseRefreshDepth,
					depthIncreaseStep, updateDelay, maxRecursionDeep, (Workspace) ResourcesPlugin.getWorkspace());
		}

		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) {
			refreshStarted = true;
			IStatus status = super.runInWorkspace(monitor);
			refreshDone = true;
			return status;
		}

		@Override
		protected List<IResource> collectChildrenToDepth(IResource resource, ArrayList<IResource> children, int depth) {
			// System.out.println("collectChildrenToDepth " + depth);// + ": " + resource);
			List<IResource> list = super.collectChildrenToDepth(resource, children, depth);
			visitedResources.add(resource);
			visitedResources.addAll(list);
			if (maxDepth < depth) {
				maxDepth = depth;
			}
			if (minDepth > depth) {
				minDepth = depth;
			}
			return list;
		}

	}

}
