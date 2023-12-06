/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
package org.eclipse.core.tests.resources;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.eclipse.core.tests.resources.ResourceTestUtil.compareContent;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.eclipse.core.internal.resources.PlatformURLResourceConnection;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

/**
 * Test suites for {@link org.eclipse.core.internal.resources.PlatformURLResourceConnection}
 */
public class ResourceURLTest extends ResourceTest {
	private final String[] resourcePaths = new String[] { "/", "/1/", "/1/1", "/1/2", "/1/3", "/2/", "/2/1", "/2/2",
			"/2/3", "/3/", "/3/1", "/3/2", "/3/3", "/4/", "/5" };

	private static final String CONTENT = "content";
	protected static IPath[] interestingPaths;
	protected static IResource[] interestingResources;

	/**
	 * Need a zero argument constructor to satisfy the test harness.
	 * This constructor should not do any real work nor should it be
	 * called by user code.
	 */
	public ResourceURLTest() {
		super();
	}

	public ResourceURLTest(String name) {
		super(name);
	}

	private void checkURL(IResource resource) throws Throwable {
		URL url = getURL(resource);
		IPath file = IPath.fromOSString(FileLocator.resolve(url).getFile());
		IPath metric = resource.getLocation();
		assertEquals(metric, file);
	}

	protected IProject getTestProject() {
		return getWorkspace().getRoot().getProject("testProject");
	}

	protected IProject getTestProject2() {
		return getWorkspace().getRoot().getProject("testProject2");
	}

	private URL getURL(IPath path) throws Throwable {
		return new URL("platform:/resource" + path.makeAbsolute().toString());
	}

	private URL getURL(IResource resource) throws Throwable {
		return getURL(resource.getFullPath());
	}

	public void testBasicURLs() throws Throwable {
		IResource[] resources = buildResources(getWorkspace().getRoot(), resourcePaths);
		createInWorkspace(resources);
		for (IResource resource : resources) {
			checkURL(resource);
		}
	}

	public void testExternalURLs() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("test");
		IProjectDescription desc = getWorkspace().newProjectDescription("test");
		desc.setLocation(Platform.getLocation().append("../testproject"));
		project.create(desc, null);
		project.open(null);
		IResource[] resources = buildResources(project, resourcePaths);
		createInWorkspace(resources);
		for (IResource resource : resources) {
			checkURL(resource);
		}
	}

	public void testNonExistantURLs() throws Throwable {
		IResource[] resources = buildResources(getWorkspace().getRoot(), resourcePaths);
		for (int i = 1; i < resources.length; i++) {
			final int index = i;
			assertThrows(IOException.class, () -> checkURL(resources[index]));
		}
	}

	/**
	 * Tests decoding of normalized URLs containing spaces
	 */
	public void testSpaces() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("My Project");
		IFile file = project.getFile("a.txt");
		createInWorkspace(file, CONTENT);
		URL url = new URL(PlatformURLResourceConnection.RESOURCE_URL_STRING + "My%20Project/a.txt");
		InputStream stream = url.openStream();
		assertTrue("1.0", compareContent(stream, createInputStream(CONTENT)));
	}
}
