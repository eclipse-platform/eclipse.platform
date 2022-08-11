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

import java.util.*;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.ResourceTest;

public class PropertyManagerPerformanceTest extends ResourceTest {

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
	private List<IResource> createTree(IFolder base, int filesPerFolder) {
		IFolder[] folders = new IFolder[5];
		folders[0] = base.getFolder("folder1");
		folders[1] = base.getFolder("folder2");
		folders[2] = folders[0].getFolder("folder3");
		folders[3] = folders[2].getFolder("folder4");
		folders[4] = folders[3].getFolder("folder5");
		List<IResource> resources = new ArrayList<>(filesPerFolder * folders.length);
		resources.addAll(Arrays.asList(folders));
		ensureExistsInWorkspace(folders, true);
		for (IFolder folder : folders) {
			for (int j = 0; j < filesPerFolder; j++) {
				IFile file = folder.getFile("file" + j);
				ensureExistsInWorkspace(file, getRandomContents());
				resources.add(file);
			}
		}
		return resources;
	}

	private void testGetProperty(int filesPerFolder, final int properties, int measurements, int repetitions) {
		IProject proj1 = getWorkspace().getRoot().getProject("proj1");
		final IFolder folder1 = proj1.getFolder("folder1");
		final List<IResource> allResources = createTree(folder1, filesPerFolder);
		for (IResource resource : allResources) {
			for (int j = 0; j < properties; j++) {
				try {
					resource.setPersistentProperty(new QualifiedName(PI_RESOURCES_TESTS, "prop" + j), getPropertyValue(200));
				} catch (CoreException ce) {
					fail("0.2", ce);
				}
			}
		}

		new PerformanceTestRunner() {
			@Override
			protected void test() {
				for (int j = 0; j < properties; j++) {
					for (IResource resource : allResources) {
						try {
							assertNotNull(resource.getPersistentProperty(new QualifiedName(PI_RESOURCES_TESTS, "prop" + j)));
						} catch (CoreException ce) {
							fail("0.2", ce);
						}
					}
				}
			}
		}.run(this, measurements, repetitions);
		try {
			((Workspace) getWorkspace()).getPropertyManager().deleteProperties(folder1, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			fail("0.1", e);
		}

	}

	public void testGetProperty100x4() {
		testGetProperty(100, 4, 10, 2);
	}

	public void testGetProperty20x20() {
		testGetProperty(20, 20, 10, 2);
	}

	public void testGetProperty4x100() {
		testGetProperty(4, 100, 10, 1);
	}

	private void testSetProperty(int filesPerFolder, int properties, int measurements, int repetitions) {
		IProject proj1 = getWorkspace().getRoot().getProject("proj1");
		final IFolder folder1 = proj1.getFolder("folder1");
		final List<IResource> allResources = createTree(folder1, filesPerFolder);
		new PerformanceTestRunner() {

			@Override
			protected void tearDown() {
				try {
					((Workspace) getWorkspace()).getPropertyManager().deleteProperties(folder1, IResource.DEPTH_INFINITE);
				} catch (CoreException e) {
					fail("0.1", e);
				}
			}

			@Override
			protected void test() {
				for (IResource resource : allResources) {
					try {
						resource.setPersistentProperty(new QualifiedName(PI_RESOURCES_TESTS, "prop" + ((int) Math.random() * 50)), getPropertyValue(200));
					} catch (CoreException ce) {
						fail("0.2", ce);
					}
				}
			}
		}.run(this, measurements, repetitions);
	}

	public void testSetProperty100x4() {
		testSetProperty(100, 4, 10, 1);
	}

	public void testSetProperty20x20() {
		testSetProperty(20, 20, 10, 4);
	}

	public void testSetProperty4x100() {
		testSetProperty(4, 100, 10, 20);
	}
}
