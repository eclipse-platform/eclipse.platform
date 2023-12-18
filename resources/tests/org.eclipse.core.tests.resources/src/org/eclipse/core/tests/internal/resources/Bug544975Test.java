/*******************************************************************************
 * Copyright (c) 2019 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.resources;

import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

public class Bug544975Test {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	@Test
	public void testBug544975ProjectOpenBackgroundRefresh() throws Exception {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject("Bug544975");
		// turn on autorefresh
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		boolean originalRefreshSetting = prefs.getBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, false);
		try {
			prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, true);
			createInWorkspace(project);
			createFile(project, "someFile.txt", "some text");
			IFile file1 = project.getFile("someFile.txt");
			assertTrue(file1.exists());
			IFile file2 = project.getFile("someOtherFile.txt");
			assertFalse(file2.exists());
			project.close(new NullProgressMonitor());

			Path projectPath = Paths.get(project.getLocationURI());
			assertTrue("Test project must exist on file system", Files.exists(projectPath));

			Path filePath = projectPath.resolve("someFile.txt");
			Files.delete(filePath);
			filePath = projectPath.resolve("someOtherFile.txt");
			Files.createFile(filePath);

			project.open(IResource.BACKGROUND_REFRESH, new NullProgressMonitor());
			Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
			Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, new NullProgressMonitor());

			file1 = project.getFile("someFile.txt");
			assertFalse("Expected deleted project resource not exist after opening with BACKGROUND_REFRESH",
					file1.exists());
			file2 = project.getFile("someOtherFile.txt");
			assertTrue("Expected new project resource to be found after opening with BACKGROUND_REFRESH",
					file2.exists());
		} finally {
			prefs.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, originalRefreshSetting);
		}
	}

	@Test
	public void testBug544975ProjectOpenWithoutBackgroundRefresh() throws Exception {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject("Bug544975");
		createInWorkspace(project);
		createFile(project, "someFile.txt", "some text");
		IFile file1 = project.getFile("someFile.txt");
		assertTrue(file1.exists());
		IFile file2 = project.getFile("someOtherFile.txt");
		assertFalse(file2.exists());
		project.close(new NullProgressMonitor());

		Path projectPath = Paths.get(project.getLocationURI());
		assertTrue("Test project must exist on file system", Files.exists(projectPath));

		Path filePath = projectPath.resolve("someFile.txt");
		Files.delete(filePath);
		filePath = projectPath.resolve("someOtherFile.txt");
		Files.createFile(filePath);

		project.open(IResource.NONE, new NullProgressMonitor());
		Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, new NullProgressMonitor());

		file1 = project.getFile("someFile.txt");
		assertTrue("Expected deleted project resource still exist after opening without BACKGROUND_REFRESH",
				file1.exists());
		file2 = project.getFile("someOtherFile.txt");
		assertFalse("Expected new project resource not to be found after opening without BACKGROUND_REFRESH",
				file2.exists());
	}

	private IFile createFile(IProject project, String fileName, String initialContents) throws CoreException {
		IFile file = project.getFile(fileName);
		file.create(null, true, createTestMonitor());
		ByteArrayInputStream stream = new ByteArrayInputStream(initialContents.getBytes());
		file.setContents(stream, IResource.FORCE, new NullProgressMonitor());
		return file;
	}

}
