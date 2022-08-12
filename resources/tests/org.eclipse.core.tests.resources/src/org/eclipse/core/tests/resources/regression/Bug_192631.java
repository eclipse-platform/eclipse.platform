/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryTree;
import org.eclipse.core.tests.internal.filesystem.remote.RemoteFileSystem;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Test for bug 192631
 */
public class Bug_192631 extends ResourceTest {
	private static final String USER_A = "userA";
	private static final String USER_B = "userB";
	private static final String HOST_A = "hostA.example.com";
	private static final String HOST_B = "hostB.example.com";
	private static final int PORT_A = 1111;
	private static final int PORT_B = 2222;

	private static final String COMMON = "/common";
	private static final String FOLDER_A = "/common/folderA";
	private static final String FOLDER_B = "/common/folderB";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		MemoryTree.TREE.deleteAll();
	}

	@Override
	protected void tearDown() throws Exception {
		MemoryTree.TREE.deleteAll();
		super.tearDown();
	}

	public void testCompareHost() throws CoreException, URISyntaxException {
		URI commonA = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_A, -1, COMMON, null, null);
		URI commonB = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_B, -1, COMMON, null, null);
		URI folderA = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_A, -1, FOLDER_A, null, null);
		URI folderB = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_B, -1, FOLDER_B, null, null);

		final Set<URI> toVisit = new HashSet<>();
		final int toVisitCount[] = new int[] {0};
		IResourceVisitor visitor = resource -> {
			toVisit.remove(resource.getLocationURI());
			toVisitCount[0]--;
			return true;
		};

		EFS.getStore(folderA).mkdir(EFS.NONE, null);
		EFS.getStore(folderB).mkdir(EFS.NONE, null);

		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		IProject projectA = workspace.getRoot().getProject("projectA");
		ensureExistsInWorkspace(projectA, true);
		IFolder linkA = projectA.getFolder("link_to_commonA");
		linkA.createLink(commonA, IResource.NONE, getMonitor());

		IProject projectB = workspace.getRoot().getProject("projectB");
		ensureExistsInWorkspace(projectB, true);
		IFolder linkB = projectB.getFolder("link_to_commonB");
		linkB.createLink(commonB, IResource.NONE, getMonitor());

		toVisit.addAll(Arrays.asList(new URI[] {projectA.getLocationURI(), commonA, folderA, projectA.getFile(".project").getLocationURI()}));
		toVisitCount[0] = 6;
		projectA.accept(visitor);
		assertTrue("1.1", toVisit.isEmpty());
		assertEquals("1.2", 0, toVisitCount[0]);

		toVisit.addAll(Arrays.asList(new URI[] {projectB.getLocationURI(), commonB, folderB, projectB.getFile(".project").getLocationURI()}));
		toVisitCount[0] = 6;
		projectB.accept(visitor);
		assertTrue("2.1", toVisit.isEmpty());
		assertEquals("2.2", 0, toVisitCount[0]);

		projectA.delete(true, getMonitor());
		projectB.delete(true, getMonitor());
	}

	public void testCompareUserInfo() throws CoreException, URISyntaxException {
		URI commonA = new URI(RemoteFileSystem.SCHEME_REMOTE, USER_A, HOST_A, -1, COMMON, null, null);
		URI commonB = new URI(RemoteFileSystem.SCHEME_REMOTE, USER_B, HOST_A, -1, COMMON, null, null);
		URI folderA = new URI(RemoteFileSystem.SCHEME_REMOTE, USER_A, HOST_A, -1, FOLDER_A, null, null);
		URI folderB = new URI(RemoteFileSystem.SCHEME_REMOTE, USER_B, HOST_A, -1, FOLDER_B, null, null);

		final Set<URI> toVisit = new HashSet<>();
		final int toVisitCount[] = new int[] {0};
		IResourceVisitor visitor = resource -> {
			toVisit.remove(resource.getLocationURI());
			toVisitCount[0]--;
			return true;
		};

		EFS.getStore(folderA).mkdir(EFS.NONE, null);
		EFS.getStore(folderB).mkdir(EFS.NONE, null);

		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		IProject projectA = workspace.getRoot().getProject("projectA");
		ensureExistsInWorkspace(projectA, true);
		IFolder linkA = projectA.getFolder("link_to_commonA");
		linkA.createLink(commonA, IResource.NONE, getMonitor());

		IProject projectB = workspace.getRoot().getProject("projectB");
		ensureExistsInWorkspace(projectB, true);
		IFolder linkB = projectB.getFolder("link_to_commonB");
		linkB.createLink(commonB, IResource.NONE, getMonitor());

		toVisit.addAll(Arrays.asList(new URI[] {projectA.getLocationURI(), commonA, folderA, projectA.getFile(".project").getLocationURI()}));
		toVisitCount[0] = 6;
		projectA.accept(visitor);
		assertTrue("1.1", toVisit.isEmpty());
		assertEquals("1.2", 0, toVisitCount[0]);

		toVisit.addAll(Arrays.asList(new URI[] {projectB.getLocationURI(), commonB, folderB, projectB.getFile(".project").getLocationURI()}));
		toVisitCount[0] = 6;
		projectB.accept(visitor);
		assertTrue("2.1", toVisit.isEmpty());
		assertEquals("2.2", 0, toVisitCount[0]);

		projectA.delete(true, getMonitor());
		projectB.delete(true, getMonitor());
	}

	public void testComparePort() throws CoreException, URISyntaxException {
		URI commonA = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_A, PORT_A, COMMON, null, null);
		URI commonB = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_A, PORT_B, COMMON, null, null);
		URI folderA = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_A, PORT_A, FOLDER_A, null, null);
		URI folderB = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_A, PORT_B, FOLDER_B, null, null);

		final Set<URI> toVisit = new HashSet<>();
		final int toVisitCount[] = new int[] {0};
		IResourceVisitor visitor = resource -> {
			toVisit.remove(resource.getLocationURI());
			toVisitCount[0]--;
			return true;
		};

		EFS.getStore(folderA).mkdir(EFS.NONE, null);
		EFS.getStore(folderB).mkdir(EFS.NONE, null);

		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		IProject projectA = workspace.getRoot().getProject("projectA");
		ensureExistsInWorkspace(projectA, true);
		IFolder linkA = projectA.getFolder("link_to_commonA");
		linkA.createLink(commonA, IResource.NONE, getMonitor());

		IProject projectB = workspace.getRoot().getProject("projectB");
		ensureExistsInWorkspace(projectB, true);
		IFolder linkB = projectB.getFolder("link_to_commonB");
		linkB.createLink(commonB, IResource.NONE, getMonitor());

		toVisit.addAll(Arrays.asList(new URI[] {projectA.getLocationURI(), commonA, folderA, projectA.getFile(".project").getLocationURI()}));
		toVisitCount[0] = 6;
		projectA.accept(visitor);
		assertTrue("1.1", toVisit.isEmpty());
		assertEquals("1.2", 0, toVisitCount[0]);

		toVisit.addAll(Arrays.asList(new URI[] {projectB.getLocationURI(), commonB, folderB, projectB.getFile(".project").getLocationURI()}));
		toVisitCount[0] = 6;
		projectB.accept(visitor);
		assertTrue("2.1", toVisit.isEmpty());
		assertEquals("2.2", 0, toVisitCount[0]);

		projectA.delete(true, getMonitor());
		projectB.delete(true, getMonitor());
	}
}
