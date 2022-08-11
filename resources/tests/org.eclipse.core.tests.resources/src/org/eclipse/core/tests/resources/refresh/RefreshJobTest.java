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

import static org.eclipse.core.internal.refresh.RefreshJob.BASE_REFRESH_DEPTH;
import static org.eclipse.core.internal.refresh.RefreshJob.DEPTH_INCREASE_STEP;
import static org.eclipse.core.internal.refresh.RefreshJob.FAST_REFRESH_THRESHOLD;
import static org.eclipse.core.internal.refresh.RefreshJob.MAX_RECURSION;
import static org.eclipse.core.internal.refresh.RefreshJob.SLOW_REFRESH_THRESHOLD;
import static org.eclipse.core.internal.refresh.RefreshJob.UPDATE_DELAY;
import static org.junit.Assert.assertArrayEquals;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.internal.refresh.RefreshJob;
import org.eclipse.core.internal.refresh.RefreshManager;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.resources.ResourceTest;
import org.eclipse.core.tests.resources.TestUtil;

/**
 * Tests for RefreshJob
 */
public class RefreshJobTest extends ResourceTest {

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

	@Override
	protected void setUp() throws Exception {
		super.setUp();
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

	@Override
	protected void tearDown() throws Exception {
		restoreRefreshJob();
		if (shouldResetDefault) {
			getPrefs().putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, defaultRefresh);
		}
		super.tearDown();
	}

	private IEclipsePreferences getPrefs() {
		return InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
	}

	/**
	 * Test to ensure that there is no endless loop on refresh
	 */
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
	 *
	 * XXX: test is disabled because it needs 400 seconds on fast SSD on Linux
	 */
	public void XtestSmallRecursionRefresh() throws Exception {
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
	 *
	 * XXX: test is disabled because it needs 250 seconds on fast SSD on Linux
	 */
	public void XtestBigRecursionDeepRefresh() throws Exception {
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
	public void testProjectRule() throws Exception {
		TestRefreshJob refreshJob = createAndReplaceDefaultJob();
		IProject project = createProject(getName());
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
				assertTrue("Refresh was not started", refreshJob.refreshStarted);
				assertFalse("Refresh should wait on rule", refreshJob.refreshDone);
				assertEquals("Should not visit anything yet", Collections.EMPTY_SET, refreshJob.visitedResources);
				Job.getJobManager().endRule(project);
				releaseRule = false;
				TestUtil.waitForJobs(getName(), 100, 1000);
				assertTrue("Refresh was not started", refreshJob.refreshStarted);
				assertTrue("Refresh was not finished", refreshJob.refreshDone);
				assertEquals("Missing refresh", expected, refreshJob.visitedResources);
			} finally {
				if (releaseRule) {
					Job.getJobManager().endRule(project);
				}
			}
		} finally {
			deleteProject(getName());
		}
	}

	// Disabled for now, is unstable
	public void XtestUnrelatedRule() throws Exception {
		TestRefreshJob refreshJob = createAndReplaceDefaultJob();
		IProject project = createProject(getName());
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
				TestUtil.waitForJobs(getName(), 10, 60_000, ResourcesPlugin.FAMILY_AUTO_REFRESH);
				assertTrue("Refresh was not started", refreshJob.refreshStarted);
				assertTrue("Refresh was not finished", refreshJob.refreshDone);
				Job.getJobManager().endRule(rule);
				releaseRule = false;
				assertEquals("Missing refresh", expected, refreshJob.visitedResources);
			} finally {
				if (releaseRule) {
					Job.getJobManager().endRule(rule);
				}
			}
		} finally {
			deleteProject(getName());
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
			assertTrue("Expect to generate some files", result.get() > 0);
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
		assertEquals("Unexpected min depth", minDepth, refreshJob.minDepth);
		assertEquals("Unexpected max depth", maxDepth, refreshJob.maxDepth);
	}

	private void assertAllResourcesRefreshed(IProject project, TestRefreshJob refreshJob) throws Exception {
		Set<IResource> resources = refreshJob.visitedResources;
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		Set<IResource> missing = new LinkedHashSet<>();
		Set<IResource> visited = new LinkedHashSet<>();
		project.accept(new IResourceVisitor() {
			@Override
			public boolean visit(IResource resource) throws CoreException {
				if (resource.getType() == IResource.FILE) {
					return true;
				}
				visited.add(resource);
				if (!resources.contains(resource)) {
					missing.add(resource);
				}
				return true;
			}
		});
		assertArrayEquals("Resources not refreshed", new IResource[0], missing.toArray());
		assertFalse("Projects should be not empty", visited.isEmpty());
		assertFalse("No resources refreshed", resources.isEmpty());
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
