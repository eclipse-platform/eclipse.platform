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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources.perf;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class PropertyManagerPerformanceTest {

	@Rule
	public TestName testName = new TestName();

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	public static String getPropertyValue(int size) {
		StringBuilder value = new StringBuilder(size);
		for (int i = 0; i < size; i++) {
			value.append((char) (Math.random() * Character.MAX_VALUE));
		}
		return value.toString();
	}

	/**
	 * Creates a tree of resources.
	 */
	private List<IResource> createTree(IFolder base, int filesPerFolder) throws CoreException {
		IFolder[] folders = new IFolder[5];
		folders[0] = base.getFolder("folder1");
		folders[1] = base.getFolder("folder2");
		folders[2] = folders[0].getFolder("folder3");
		folders[3] = folders[2].getFolder("folder4");
		folders[4] = folders[3].getFolder("folder5");
		List<IResource> resources = new ArrayList<>(filesPerFolder * folders.length);
		resources.addAll(Arrays.asList(folders));
		createInWorkspace(folders);
		for (IFolder folder : folders) {
			for (int j = 0; j < filesPerFolder; j++) {
				IFile file = folder.getFile("file" + j);
				createInWorkspace(file, createRandomString());
				resources.add(file);
			}
		}
		return resources;
	}

	private void testGetProperty(int filesPerFolder, final int properties, int measurements, int repetitions)
			throws Exception {
		IProject proj1 = getWorkspace().getRoot().getProject("proj1");
		final IFolder folder1 = proj1.getFolder("folder1");
		final List<IResource> allResources = createTree(folder1, filesPerFolder);
		for (IResource resource : allResources) {
			for (int j = 0; j < properties; j++) {
				resource.setPersistentProperty(new QualifiedName(PI_RESOURCES_TESTS, "prop" + j),
						getPropertyValue(200));
			}
		}

		new PerformanceTestRunner() {
			@Override
			protected void test() throws CoreException {
				for (int j = 0; j < properties; j++) {
					for (IResource resource : allResources) {
						assertNotNull(
								resource.getPersistentProperty(new QualifiedName(PI_RESOURCES_TESTS, "prop" + j)));
					}
				}
			}
		}.run(getClass(), testName.getMethodName(), measurements, repetitions);
		((Workspace) getWorkspace()).getPropertyManager().deleteProperties(folder1, IResource.DEPTH_INFINITE);

	}

	@Test
	public void testGetProperty100x4() throws Exception {
		testGetProperty(100, 4, 10, 2);
	}

	@Test
	public void testGetProperty20x20() throws Exception {
		testGetProperty(20, 20, 10, 2);
	}

	@Test
	public void testGetProperty4x100() throws Exception {
		testGetProperty(4, 100, 10, 1);
	}

	private void testSetProperty(int filesPerFolder, int properties, int measurements, int repetitions)
			throws Exception {
		IProject proj1 = getWorkspace().getRoot().getProject("proj1");
		final IFolder folder1 = proj1.getFolder("folder1");
		final List<IResource> allResources = createTree(folder1, filesPerFolder);
		new PerformanceTestRunner() {

			@Override
			protected void tearDown() throws CoreException {
				((Workspace) getWorkspace()).getPropertyManager().deleteProperties(folder1, IResource.DEPTH_INFINITE);
			}

			@Override
			protected void test() throws CoreException {
				for (IResource resource : allResources) {
					resource.setPersistentProperty(
							new QualifiedName(PI_RESOURCES_TESTS, "prop" + ((int) Math.random() * 50)),
							getPropertyValue(200));
				}
			}
		}.run(getClass(), testName.getMethodName(), measurements, repetitions);
	}

	@Test
	public void testSetProperty100x4() throws Exception {
		testSetProperty(100, 4, 10, 1);
	}

	@Test
	public void testSetProperty20x20() throws Exception {
		testSetProperty(20, 20, 10, 4);
	}

	@Test
	public void testSetProperty4x100() throws Exception {
		testSetProperty(4, 100, 10, 20);
	}

}
